import sqlite3
import json
import argparse
import sys
from contextlib import closing

# Mapping of Wiktextract POS tags to human‑readable names
POS_MAPPING = {
    "abbr": "abbreviation",
    "adj": "adjective",
    "punct": "punctuation",
    "adv": "adverb",
    "aux": "auxiliary",
    "conj": "conjunction",
    "det": "determiner",
    "num": "numeral",
    "part": "particle",
    "intj": "interjection",
    "prep": "preposition",
    "pron": "pronoun",
}


def create_database_schema(conn):
    print("Creating database schema...")
    with closing(conn.cursor()) as cursor:
        # Updated schema: combine comparative and superlative into 'compare'
        cursor.execute("""
        CREATE TABLE IF NOT EXISTS entries (
            id INTEGER PRIMARY KEY,
            word TEXT NOT NULL COLLATE NOCASE,
            part_of_speech TEXT NOT NULL,
            etymology TEXT,
            pronunciation TEXT,
            definitions TEXT,
            examples TEXT,
            synonyms TEXT,
            antonyms TEXT,
            hypernyms TEXT,
            hyponyms TEXT,
            holonyms TEXT,
            meronyms TEXT,
            derived TEXT,
            related TEXT,
            plural TEXT,
            compare TEXT,
            tenses TEXT
        )
        """)

        cursor.execute("""
        CREATE TABLE IF NOT EXISTS entry_words (
            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            word TEXT COLLATE NOCASE
        )
        """)

        print("Creating indexes...")
        cursor.execute("CREATE INDEX IF NOT EXISTS index_entries_word ON entries (word)")
        cursor.execute("CREATE INDEX IF NOT EXISTS index_entry_word ON entry_words (word)")

        conn.commit()
        print("Database schema and indexes created successfully.")


def process_json_dump(json_dump_path, db_path):
    conn = sqlite3.connect(db_path)
    cursor = conn.cursor()

    create_database_schema(conn)

    word_count = 0
    commit_interval = 5000

    print(f"\nStarting processing of {json_dump_path}. This may take some time...")

    entries = []

    try:
        with open(json_dump_path, 'r', encoding='utf-8') as f:
            for line in f:
                try:
                    word_data = json.loads(line)
                    word_str = word_data.get("word")
                    pos = word_data.get("pos")

                    if not word_str or not pos:
                        continue

                    entries.append((word_str, word_data))
                except json.JSONDecodeError:
                    print(f"\nSkipping line due to JSON decoding error: {line.strip()}", file=sys.stderr)

        print("Sorting entries (case-insensitive)...")
        entries.sort(key=lambda x: x[0].lower())
        print("Sorting complete.")

        for word_str, word_data in entries:
            try:
                cursor.execute("INSERT OR IGNORE INTO entry_words (word) VALUES (?)", (word_str,))

                pos = POS_MAPPING.get(word_data.get("pos"), word_data.get("pos"))
                etymology = word_data.get("etymology_text")
                pron_ipa = None
                if sounds := word_data.get("sounds"):
                    for sound in sounds:
                        if ipa := sound.get("ipa"):
                            pron_ipa = ipa
                            break

                definitions_list = []
                examples_list = []
                if senses := word_data.get("senses"):
                    for sense in senses:
                        definitions_list.extend(sense.get("glosses", []))
                        for example in sense.get("examples", []):
                            if example_text := example.get("text"):
                                examples_list.append(example_text)

                # Extract relations
                relation_mapping = {
                    "synonyms": "synonyms",
                    "antonyms": "antonyms",
                    "hypernyms": "hypernyms",
                    "hyponyms": "hyponyms",
                    "holonyms": "holonyms",
                    "meronyms": "meronyms",
                    "derived": "derived",
                    "related": "related"
                }

                relations_data = {}
                for rel_type, json_key in relation_mapping.items():
                    relations_data[rel_type] = None
                    if relations := word_data.get(json_key):
                        related_words = []
                        for r in relations:
                            if isinstance(r, dict) and r.get("word"):
                                related_words.append(r["word"])
                            elif isinstance(r, str):
                                related_words.append(r)
                        if related_words:
                            relations_data[rel_type] = json.dumps(related_words)

                # Extract forms: plural, comparative, superlative, and verb tenses
                forms_data = word_data.get("forms")
                plural = None
                comp = None
                sup = None

                # Initialize a list to hold all tense forms as simple strings
                tenses_list = []

                if isinstance(forms_data, dict):
                    plural = forms_data.get("plural")
                    comp = forms_data.get("comparative")
                    sup = forms_data.get("superlative")
                elif isinstance(forms_data, list):
                    for form in forms_data:
                        if not isinstance(form, dict):
                            continue
                        form_text = form.get("form")
                        tags = form.get("tags") or []
                        if not form_text or not tags:
                            continue

                        if "plural" in tags:
                            plural = form_text
                        if "comparative" in tags:
                            comp = form_text
                        if "superlative" in tags:
                            sup = form_text

                        if any(tense_tag in tags for tense_tag in ["third-person", "past-participle", "present-participle", "gerund", "past"]):
                            if form_text not in tenses_list:
                                tenses_list.append(form_text)

                # Combine comparative and superlative into one list
                compare_list = []
                if comp:
                    compare_list.append(comp)
                if sup:
                    compare_list.append(sup)

                compare_json = json.dumps(compare_list) if compare_list else None
                tenses_json = json.dumps(tenses_list) if tenses_list else None

                cursor.execute(
                    """
                    INSERT INTO entries (
                        word, part_of_speech, etymology, pronunciation,
                        definitions, examples,
                        synonyms, antonyms, hypernyms,
                        hyponyms, holonyms, meronyms,
                        derived, related,
                        plural, compare, tenses
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    (
                        word_str, pos, etymology, pron_ipa,
                        json.dumps(definitions_list) if definitions_list else None,
                        json.dumps(examples_list) if examples_list else None,
                        relations_data["synonyms"], relations_data["antonyms"],
                        relations_data["hypernyms"], relations_data["hyponyms"],
                        relations_data["holonyms"], relations_data["meronyms"],
                        relations_data["derived"], relations_data["related"],
                        plural, compare_json, tenses_json
                    )
                )

                word_count += 1
                if word_count % commit_interval == 0:
                    conn.commit()
                    print(f"Processed and committed {word_count} words...", end='\r')

            except Exception as e:
                print(f"\nAn error occurred while processing word '{word_str}': {e}", file=sys.stderr)
                conn.rollback()

    except FileNotFoundError:
        print(f"Error: The file was not found at {json_dump_path}", file=sys.stderr)
        sys.exit(1)

    conn.commit()
    print(f"\nExtraction complete. Total words processed: {word_count}")
    conn.close()


def main():
    parser = argparse.ArgumentParser(
        description="Process a Wiktionary JSON dump into a two-table SQLite database.",
        formatter_class=argparse.RawTextHelpFormatter
    )
    parser.add_argument(
        "dump_file",
        default="kaikki.org-dictionary-English-words.jsonl",
        nargs='?',
        help="Path to the Wiktionary JSON dump file (default: kaikk...jsonl)"
    )
    parser.add_argument(
        "db_file",
        default="wiktionary_database.db",
        nargs='?',
        help="Path for the output SQLite database file (default: wiktionary_database.db)"
    )
    args = parser.parse_args()

    process_json_dump(args.dump_file, args.db_file)
    print(f"\nProcess finished. Database saved to {args.db_file}")


if __name__ == "__main__":
    main()
