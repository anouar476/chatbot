import os
import openai
from PyPDF2 import PdfReader
import fitz  # PyMuPDF
import chromadb
from chromadb.utils.embedding_functions import OpenAIEmbeddingFunction

class PDFQASystem:
    def __init__(self, pdf_path, openai_api_key):
        # Validate inputs
        if not openai_api_key:
            raise ValueError("OpenAI API key is required")

        if not pdf_path or not os.path.exists(pdf_path):
            raise FileNotFoundError(f"PDF file not found at path: {pdf_path}")

        # Set up OpenAI API
        self.client = openai.OpenAI(api_key=openai_api_key)

        # Initialize Chroma client
        self.chroma_client = chromadb.EphemeralClient()

        # Set up embedding function
        self.embedding_function = OpenAIEmbeddingFunction(api_key=openai_api_key)

        # Create collection
        self.collection = self.chroma_client.create_collection(
            name="pdf_data",
            embedding_function=self.embedding_function
        )

        # Store PDF path
        self.pdf_path = pdf_path
        self.previous_questions = []
        self.image_paths = []

    def extract_text_from_pdf(self):
        """Extract text from PDF file."""
        text = ""
        try:
            reader = PdfReader(self.pdf_path)
            for page in reader.pages:
                text += page.extract_text() or ""

            if not text.strip():
                print("⚠️ Warning: No text could be extracted from the PDF.")

            return text
        except Exception as e:
            print(f"❌ Error extracting PDF text: {e}")
            return ""

    def extract_images_from_pdf(self):
        """Extract images from PDF file."""
        image_dir = "extracted_images"
        os.makedirs(image_dir, exist_ok=True)

        try:
            doc = fitz.open(self.pdf_path)
            for page_num in range(len(doc)):
                page = doc[page_num]
                images = page.get_images(full=True)
                for img_index, img in enumerate(images):
                    xref = img[0]
                    base_image = doc.extract_image(xref)
                    image_bytes = base_image["image"]
                    image_filename = os.path.join(image_dir, f"page_{page_num + 1}_img_{img_index + 1}.png")
                    with open(image_filename, "wb") as img_file:
                        img_file.write(image_bytes)
                    self.image_paths.append(image_filename)

            if not self.image_paths:
                print("⚠️ Warning: No images could be extracted from the PDF.")
        except Exception as e:
            print(f"❌ Error extracting images: {e}")

    def chunk_text(self, text, chunk_size=500):
        """Split text into manageable chunks."""
        words = text.split()
        chunks = []
        for i in range(0, len(words), chunk_size):
            chunks.append(" ".join(words[i:i + chunk_size]))
        return chunks

    def store_pdf_in_chroma(self):
        """Store PDF text chunks in Chroma database."""
        pdf_text = self.extract_text_from_pdf()

        if not pdf_text.strip():
            print("❌ Cannot store empty PDF content.")
            return False

        chunks = self.chunk_text(pdf_text)

        # Add chunks to collection
        for i, chunk in enumerate(chunks):
            self.collection.add(
                documents=[chunk],
                embeddings=self.embedding_function([chunk]),
                metadatas=[{"source": "pdf", "page": i + 1}],
                ids=[f"chunk_{i + 1}"]

            )

        print(f"✅ Stored {len(chunks)} text chunks in database.")
        return True

    def query_relevant_chunks(self, question, n_results=3):
        """Retrieve relevant text chunks for a question."""
        # Adjust n_results to the actual number of chunks if it exceeds available chunks
        total_chunks = len(self.collection.get()['ids'])
        n_results = min(n_results, total_chunks)

        results = self.collection.query(
            query_texts=[question],
            n_results=n_results
        )
        return results['documents'][0] if results['documents'] else []
    def generate_answer(self, question, context):
        """Generate an answer using OpenAI's GPT model."""
        try:
            prompt = (
                "You are a highly knowledgeable and resourceful assistant specializing in Java programming, computer science, and related fields. "
                "Your task is to provide accurate, insightful, and contextually relevant answers to the following questions based on the context provided. "
                "If the context doesn't fully cover the question, respond by using your expertise to offer a helpful, thoughtful answer. "
                "Whenever possible, include additional explanations or relevant resources to enhance the answer. "
                "If there are images from the PDF that are relevant, include them in your response.\n\n"
                f"Context: {context}\n\n"
                f"Question: {question}\n\n"
                "Answer:"
            )
            response = self.client.chat.completions.create(
                model="gpt-3.5-turbo",
                messages=[
                    {"role": "user", "content": prompt}
                ],
                max_tokens=300
            )
            return response.choices[0].message.content
        except Exception as e:
            print(f"Error generating answer: {e}")
            return "I'm unable to generate an answer at the moment."

    def interactive_qa(self):
        """Start interactive Q&A session."""
        # Prepare database first
        if not self.store_pdf_in_chroma():
            print("❌ Could not initialize PDF database. Exiting.")
            return

        self.extract_images_from_pdf()  # Extract images for reference

        print("\n📚 i'm your teacher now ! Ask questions about JAVA PROGRAMMING LANGAUAGES COURSE.")
        print("Type 'exit' to quit.\n")

        while True:
            try:
                question = input("🤔 Your question: ").strip()

                if question.lower() in ['exit', 'quit', 'q']:
                    print("Thank you for using the PDF Q&A system. Goodbye!")
                    break

                # Find relevant context
                context = self.query_relevant_chunks(question)
                context_text = "\n".join(context)

                # Generate answer
                if context:
                    answer = self.generate_answer(question, context_text)
                    print(f"\n🌟 Answer: {answer}\n")
                else:
                    print("❌ No relevant information found in the PDF.\n")

                # Store the question for context
                self.previous_questions.append(question)

            except KeyboardInterrupt:
                print("\nOperation cancelled. Type 'exit' to quit.")
            except Exception as e:
                print(f"An unexpected error occurred: {e}")

def main():
    # Verify PDF path and OpenAI key
    PDF_PATH = "C:/Users/pc/IdeaProjects/demo1/src/main/java/com/example/demo1/td.pdf"  # Ensure this file exists
    OPENAI_API_KEY = "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"

    try:
        qa_system = PDFQASystem(PDF_PATH, OPENAI_API_KEY)
        qa_system.interactive_qa()
    except FileNotFoundError as e:
        print(f"❌ File Error: {e}")
        print("Please ensure the PDF file exists in the current directory.")
    except ValueError as e:
        print(f"❌ Configuration Error: {e}")
    except Exception as e:
        print(f"❌ Unexpected Error: {e}")
if __name__ == "__main__":
    main()