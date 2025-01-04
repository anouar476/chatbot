from flask import Flask, request, jsonify
from test import PDFQASystem
import os

app = Flask(__name__)

# Initialize PDF Q&A System with multiple PDFs
PDF_PATHS = ["data/td1.pdf", "data/td.pdf", "data/1.pdf", "data/2.pdf", "data/3.pdf", "data/4.pdf", "data/5.pdf", "data/6.pdf", "data/listetu.pdf"]  # Ensure these files exist
OPENAI_API_KEY = os.getenv("OPENAI_API_KEY")
qa_system = None
ENSET_KEYWORDS = [
    "enset", "école", "formation", "département", "étudiant", "professeur ",
    "cours", "examen", "td", "tp", "stage", "inscription", "admission",
    "filière", "emploi du temps", "note", "absence", "bibliothèque",
    "administration", "scolarité",
    # English keywords
    "school", "course", "department", "student", "professor", "teachers", "exam", 
    "training", "schedule", "registration", "admission", "grade", "absence", ''
    "library", "administration", "study", "class",
    # Arabic keywords
    "المدرسة", "دورة", "قسم", "طالب", "أستاذ", "امتحان", "تدريب", "جدول", 
    "تسجيل", "قبول", "درجة", "غياب", "مكتبة", "إدارة", "دراسة", "فصل"
]

def is_enset_related(question):
    """Check if the question is related to ENSET based on keywords."""
    question_lower = question.lower()
    return any(keyword in question_lower for keyword in ENSET_KEYWORDS)


try:
    if not OPENAI_API_KEY:
        raise ValueError("OpenAI API key is required")

    existing_pdfs = [path for path in PDF_PATHS if os.path.exists(path)]
    if existing_pdfs:
        qa_system = PDFQASystem(existing_pdfs, OPENAI_API_KEY)
        # Preprocess the PDF data
        qa_system.store_pdfs_in_chroma()
        qa_system.extract_images_from_pdfs()
    else:
        raise FileNotFoundError("None of the specified PDF files were found.")
except Exception as e:
    print(f"Error initializing the PDF Q&A system: {e}")

def format_answer(answer, is_error=False):
    """Format the answer with appropriate emojis and structure."""
    if is_error:
        return {
            "answer": "️⚠️ Votre message est hors sujet ou manque de clarté. Je suis ici pour vous aider à savoir toutes les informations concernant ENSET. 🎓\n\n💡 Conseil: Posez des questions spécifiques sur ENSET, ses formations, ses départements ou ses services."
        }
    
    # Add relevant emojis based on content
    formatted_answer = f"🎓 ENSET Assistant\n\n"
    
    # Add category emoji based on content keywords
    if "département" in answer.lower():
        formatted_answer += "🏢 "
    elif "cours" in answer.lower() or "formation" in answer.lower():
        formatted_answer += "📚 "
    elif "professeur" in answer.lower():
        formatted_answer += "👨‍🏫 "
    elif "étudiant" in answer.lower():
        formatted_answer += "👨‍🎓 "
    elif "examen" in answer.lower() or "note" in answer.lower():
        formatted_answer += "📝 "
    
    formatted_answer += f"{answer}\n\n💡 N'hésitez pas à poser d'autres questions sur ENSET!"
    
    return {"answer": formatted_answer}

@app.route('/ask', methods=['POST'])
def ask():
    """
    Endpoint to handle chatbot questions about ENSET.
    Receives a JSON payload: {"question": "your question"}
    Returns a JSON response with a formatted answer
    """
    if not qa_system:
        return jsonify({"error": "PDF Q&A System is not initialized."}), 500

    data = request.json
    if not data or 'question' not in data:
        return jsonify({"error": "Invalid request, 'question' is required."}), 400

    question = data['question']

    # Check if the question is ENSET-related
    if not is_enset_related(question):
        # Return a more informative message when the question is off-topic
        return jsonify({
            "answer": "⚠️ Désolé, je ne peux répondre qu'aux questions liées à ENSET. 🎓\n\n💡 Conseil: Posez des questions spécifiques sur ENSET, ses formations, ses départements ou ses services."
        })

    try:
        # Find relevant context
        context = qa_system.query_relevant_chunks(question)
        context_text = "\n".join(context)

        # Generate the answer
        if context:
            answer = qa_system.generate_answer(question, context_text)
            return jsonify(format_answer(answer))
        else:
            return jsonify(format_answer("", is_error=True))
      
    except Exception as e:
        print(f"Error processing the question: {e}")
        return jsonify({"error": "Une erreur s'est produite lors du traitement de votre demande."}), 500

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000, debug=True)