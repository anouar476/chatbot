import os
import numpy as np
import openai
import faiss
from PyPDF2 import PdfReader
from flask import Flask, request, jsonify
from typing import List, Dict, Any
from dataclasses import dataclass
import nltk
from nltk.tokenize import sent_tokenize
from nltk.corpus import stopwords
import re
from datetime import datetime
import logging


try:
    nltk.download('punkt')
    nltk.download('stopwords')
    nltk.download('punkt_tab')
except Exception as e:
    print(f"Error downloading NLTK data: {e}")


logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    handlers=[
        logging.FileHandler('qa_system.log'),
        logging.StreamHandler()
    ]
)
logger = logging.getLogger(__name__)

@dataclass
class ChunkInfo:
    text: str
    source: str
    page_number: int
    relevance_score: float = 0.0

class PDFQASystem:
    def __init__(self, pdf_paths: List[str], openai_api_key: str):
        if not openai_api_key:
            raise ValueError("OpenAI API key is required")

        if not pdf_paths or not all(os.path.exists(pdf) for pdf in pdf_paths):
            raise FileNotFoundError("One or more PDF files not found. Please check the file paths.")

        openai.api_key = openai_api_key
        
        
        self.index = None
        self.pdf_paths = pdf_paths
        self.embedding_function = self.get_embedding_function()
        
        
        self.chunks: List[ChunkInfo] = []
        self.chunk_embeddings = []
        
        self.load_pdfs_and_store_in_faiss()
        logger.info("PDF QA System initialized successfully")

    def get_embedding_function(self):
        """Enhanced embedding function with error handling and retries."""
        class OpenAIEmbeddingFunction:
            def __call__(self, input):
                return self.get_embeddings(input)
            
            def get_embeddings(self, texts, max_retries=3):
                for attempt in range(max_retries):
                    try:
                        response = openai.Embedding.create(
                            model="text-embedding-ada-002",
                            input=texts
                        )
                        return [np.array(embedding['embedding']) for embedding in response['data']]
                    except Exception as e:
                        if attempt == max_retries - 1:
                            logger.error(f"Failed to get embeddings after {max_retries} attempts: {e}")
                            raise
                        logger.warning(f"Embedding attempt {attempt + 1} failed: {e}")
                        continue

        return OpenAIEmbeddingFunction()

    def chunk_text(self, text: str, chunk_size: int = 500) -> List[str]:
        """Enhanced text chunking with sentence boundary respect."""
        sentences = sent_tokenize(text)
        chunks = []
        current_chunk = []
        current_size = 0
        
        for sentence in sentences:
            sentence_words = sentence.split()
            if current_size + len(sentence_words) <= chunk_size:
                current_chunk.append(sentence)
                current_size += len(sentence_words)
            else:
                if current_chunk:
                    chunks.append(" ".join(current_chunk))
                current_chunk = [sentence]
                current_size = len(sentence_words)
        
        if current_chunk:
            chunks.append(" ".join(current_chunk))
        
        return chunks

    def extract_text_from_pdf(self, pdf_path: str) -> Dict[int, str]:
        """Extract text from PDF with page tracking."""
        pages_text = {}
        try:
            reader = PdfReader(pdf_path)
            for page_num, page in enumerate(reader.pages, 1):
                text = page.extract_text() or ""
                if text.strip():
                    pages_text[page_num] = text
            
            if not pages_text:
                logger.warning(f"No text extracted from PDF: {pdf_path}")
            return pages_text
        except Exception as e:
            logger.error(f"Error extracting text from PDF {pdf_path}: {e}")
            return {}

    def load_pdfs_and_store_in_faiss(self):
        """Load PDFs and store in FAISS with enhanced metadata."""
        for pdf_path in self.pdf_paths:
            pages_text = self.extract_text_from_pdf(pdf_path)
            for page_num, text in pages_text.items():
                chunks = self.chunk_text(text)
                for chunk in chunks:
                    self.chunks.append(ChunkInfo(
                        text=chunk,
                        source=os.path.basename(pdf_path),
                        page_number=page_num
                    ))

        chunk_texts = [chunk.text for chunk in self.chunks]
        self.chunk_embeddings = np.array(self.embedding_function(chunk_texts)).astype('float32')

        dimension = self.chunk_embeddings.shape[1]
        self.index = faiss.IndexFlatIP(dimension)  
        faiss.normalize_L2(self.chunk_embeddings) 
        self.index.add(self.chunk_embeddings)

    def query_relevant_chunks(self, question: str, n_results: int = 3) -> List[ChunkInfo]:
        """Query FAISS with enhanced relevance scoring."""
        question_embedding = self.embedding_function([question])[0]
        question_embedding = np.array([question_embedding]).astype('float32')
        faiss.normalize_L2(question_embedding)

        
        scores, indices = self.index.search(question_embedding, n_results)
        
        relevant_chunks = []
        for idx, score in zip(indices[0], scores[0]):
            chunk = self.chunks[idx]
            chunk.relevance_score = float(score)
            relevant_chunks.append(chunk)
        
        return relevant_chunks

    def generate_answer(self, question: str, context: List[ChunkInfo]) -> Dict[str, Any]:
        """Generate enhanced answer with metadata and confidence scoring."""
        try:
            context_text = "\n".join([f"[Source: {chunk.source}, Page: {chunk.page_number}]\n{chunk.text}" for chunk in context])
            
            
            context_texts = [chunk.text for chunk in context]
            context_embeddings = np.array(self.embedding_function(context_texts)).astype('float32')
            question_embedding = np.array([self.embedding_function([question])[0]]).astype('float32')
            faiss.normalize_L2(context_embeddings)
            faiss.normalize_L2(question_embedding)
            
            similarities = np.dot(context_embeddings, question_embedding.T)
            avg_similarity = np.mean(similarities)

            if avg_similarity < 0.1:
                return {
                    "answer": "⚠️ Votre message est hors sujet ou manque de clarté. Je suis ici pour vous aider à savoir toutes les informations concernant ENSET 🎓.",
                    "confidence": 0.0,
                    "is_relevant": False
                }
            
            prompt = (
                "Tu es un assistant spécialisé pour ENSET Mohammedia. "
                "Réponds aux questions en te basant sur le contexte fourni. "
                "Si le contexte ne permet pas de répondre précisément, indique-le clairement. "
                "Sois précis, professionnel et utile. , et repondre par la language utilise dans la reponce \n\n"
                f"Contexte:\n{context_text}\n\n"
                f"Question: {question}\n\n"
                "Réponse:"
            )

            response = openai.ChatCompletion.create(
                model="gpt-4",
                messages=[{"role": "user", "content": prompt}],
                max_tokens=500,
                temperature=0.7
            )
            
            answer = response.choices[0].message['content']
            confidence = sum(chunk.relevance_score for chunk in context) / len(context)
            
            return {
                "answer": answer,
                "confidence": float(confidence),
                "is_relevant": True,
                "sources": [{"file": chunk.source, "page": chunk.page_number} for chunk in context]
            }
        except Exception as e:
            logger.error(f"Error generating answer: {e}")
            return {
                "answer": "Désolé, une erreur s'est produite lors de la génération de la réponse.",
                "confidence": 0.0,
                "is_relevant": True,
                "error": str(e)
            }

app = Flask(__name__)

@app.route('/ask', methods=['POST'])
def ask():
    """Enhanced endpoint with better error handling and response formatting."""
    if not hasattr(app, 'qa_system'):
        return jsonify({"error": "System not initialized"}), 500

    try:
        data = request.get_json()
        if not data or 'question' not in data:
            return jsonify({"error": "Invalid request. 'question' field is required."}), 400

        question = data['question']
        
        
        relevant_chunks = app.qa_system.query_relevant_chunks(question)
        
        if relevant_chunks:
            response = app.qa_system.generate_answer(question, relevant_chunks)
            return jsonify(response)
        else:
            return jsonify({
                "answer": "Désolé, je n'ai trouvé aucune information pertinente dans mes documents.",
                "confidence": 0.0,
                "is_relevant": False
            })
    except Exception as e:
        logger.error(f"Error during ask request: {e}")
        return jsonify({"error": "An error occurred while processing the request."}), 500

if __name__ == '__main__':
    
    pdf_paths = ["data/depa.pdf", "data/td.pdf", "data/1.pdf", "data/2.pdf", "data/3.pdf", "data/4.pdf", "data/5.pdf", "data/6.pdf", "data/listetu.pdf"]
    openai_api_key = os.getenv("OPENAI_API_KEY")  
    app.qa_system = PDFQASystem(pdf_paths, openai_api_key)
    app.run(debug=True)
