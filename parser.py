import sqlite3
import json
import argparse
import sys
from contextlib import closing

# Mapping of POS abbreviations to full forms
POS_MAPPING = {
    "ab": "abbreviation",
    "adj": "adjective",
    "adv": "adverb",
    "af": "affix",
    "ch": "character",
    "cr": "circumfix",
    "cn": "conjunction",
    "dt": "determiner",
    "inf": "infix",
    "intf": "interfix",
    "int": "interjection",
    "nm": "name",
    "num": "numeral",
    "prt": "particle",
    "ph": "phrase",
    "pp": "post position",
    "prf": "prefix",
    "prp": "preposition",
    "prpp": "prepositional phrase",
    "prn": "pronoun",
    "prv": "proverb",
    "pct": "punctuation",
    "sf": "suffix",
    "sm": "symbol",
}


def create_database_schema(conn):
    print("Creating single-table database schema with separate relation columns...")
    with closing(conn.cursor()) as cursor:
        cursor.execute("""
        CREATE TABLE IF NOT EXISTS entries (
            id INTEGER PRIMARY KEY,
            word TEXT NOT NULL,
            pos TEXT NOT NULL,
            etymology TEXT,
            pronunciation_ipa TEXT,
            definitions TEXT,
            examples TEXT,
            synonyms TEXT,
            antonyms TEXT,
            hypernyms TEXT,
            hyponyms TEXT,
            holonyms TEXT,
            meronyms TEXT,
            derived TEXT,
            related TEXT
        )
        """)

        print("Creating indexes...")
        cursor.execute("CREATE INDEX IF NOT EXISTS idx_word ON entries (word)")

        conn.commit()
        print("Database schema and indexes created successfully.")


def process_json_dump(json_dump_path, db_path):
    conn = sqlite3.connect(db_path)
    cursor = conn.cursor()

    create_database_schema(conn)

    word_count = 0
    commit_interval = 5000

    print(f"\nStarting processing of {json_dump_path}. This may take some time...")

    try:
        with open(json_dump_path, 'r', encoding='utf-8') as f:
            for line in f:
                try:
                    word_data = json.loads(line)
                    word_str = word_data.get("word")
                    pos = word_data.get("pos")

                    if not word_str or not pos:
                        continue

                    # Normalize POS using the mapping
                    pos = POS_MAPPING.get(pos, pos)

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

                    cursor.execute(
                        """
                        INSERT INTO entries (
                            word, pos, etymology, pronunciation_ipa,
                            definitions, examples,
                            synonyms, antonyms, hypernyms,
                            hyponyms, holonyms, meronyms,
                            derived, related
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                        (
                            word_str, pos, etymology, pron_ipa,
                            json.dumps(definitions_list) if definitions_list else None,
                            json.dumps(examples_list) if examples_list else None,
                            relations_data["synonyms"], relations_data["antonyms"],
                            relations_data["hypernyms"], relations_data["hyponyms"],
                            relations_data["holonyms"], relations_data["meronyms"],
                            relations_data["derived"], relations_data["related"]
                        )
                    )

                    word_count += 1
                    if word_count % commit_interval == 0:
                        conn.commit()
                        print(f"Processed and committed {word_count} words...", end='\r')

                except json.JSONDecodeError:
                    print(f"\nSkipping line due to JSON decoding error: {line.strip()}", file=sys.stderr)
                except Exception as e:
                    print(f"\nAn error occurred while processing word '{word_data.get('word')}': {e}", file=sys.stderr)
                    conn.rollback()

    except FileNotFoundError:
        print(f"Error: The file was not found at {json_dump_path}", file=sys.stderr)
        sys.exit(1)

    conn.commit()
    print(f"\nExtraction complete. Total words processed: {word_count}")
    conn.close()


def main():
    parser = argparse.ArgumentParser(
        description="Process a Wiktionary JSON dump into a single-table SQLite database.",
        formatter_class=argparse.RawTextHelpFormatter
    )
    parser.add_argument(
        "dump_file",
        default="kaikki.org-dictionary-English-words.jsonl",
        nargs='?',
        help="Path to the Wiktionary JSON dump file (default: kaikki.org-dictionary-English-words.jsonl)"
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
