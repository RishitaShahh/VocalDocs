# tts_script.py
import sys
from gtts import gTTS
import os
import traceback

def main():
    if len(sys.argv) < 3:
        print("Usage: python tts_script.py <input_text_file> <output_mp3_file>")
        sys.exit(1)

    input_file = sys.argv[1]
    output_file = sys.argv[2]

    try:
        # Check if file exists
        if not os.path.isfile(input_file):
            raise FileNotFoundError(f"Input file not found: {input_file}")

        # Read text in UTF-8
        with open(input_file, 'r', encoding='utf-8') as f:
            text = f.read().strip()

        if not text:
            raise ValueError("Input file is empty")

        # Split text into 2000-character chunks (gTTS limit)
        chunk_size = 2000
        chunks = [text[i:i+chunk_size] for i in range(0, len(text), chunk_size)]

        # Generate audio for each chunk
        for idx, chunk in enumerate(chunks):
            tts = gTTS(text=chunk, lang='en', slow=False)
            if len(chunks) > 1:
                tts.save(f"{output_file}_{idx+1}.mp3")  # Numbered if multiple chunks
            else:
                tts.save(output_file)

        print("SUCCESS: Audio conversion completed!")

    except Exception as e:
        print("TTS Conversion failed!", file=sys.stderr)
        print(f"Reason: {e}", file=sys.stderr)
        traceback.print_exc()
        sys.exit(1)

if __name__ == "__main__":
    main()