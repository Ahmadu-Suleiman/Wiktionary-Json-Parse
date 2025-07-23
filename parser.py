# -*- coding: utf-8 -*-
"""
Wiktionary JSON to SQLite Importer

This script processes a local Wiktionary JSON dump file (one JSON object per line)
and saves the data into a single, denormalized SQLite table.

In this version, relations like synonyms and antonyms are stored in their own
dedicated columns as JSON-encoded arrays of words.

Prerequisites:
1. Download a pre-processed Wiktionary JSON dump. A great source is Kaikki.org:
   https://kaikki.org/dictionary/English/index.html
   You need the file 'kaikki.org-dictionary-English-words.jsonl'. Place it in your
   project folder or provide the path to it.

Usage:
   python this_script_name.py kaikki.org-dictionary-English-words.jsonl wiktionary_database.db

"""
import sqlite3
import json
import argparse
import sys
from contextlib import closing


def create_database_schema(conn):
    """
    Creates a single table with dedicated relation columns and its indexes.

    Args:
        conn: An active sqlite3 connection object.
    """
    print("Creating single-table database schema with separate relation columns...")
    with closing(conn.cursor()) as cursor:
        # A single, denormalized table for all wiktionary data.
        cursor.execute("""
        CREATE TABLE IF NOT EXISTS entries (
            id INTEGER PRIMARY KEY,
            word TEXT NOT NULL,
            pos TEXT NOT NULL, -- Part of Speech
            etymology TEXT,
            pronunciation_ipa TEXT,
            definitionsTEXT, -- JSON list of strings
            examplesTEXT,    -- JSON list of strings
            formsTEXT,       -- JSON list of objects
            -- Dedicated columns for each relation type (simple arrays of words)
            synonymsTEXT,
            antonymsTEXT,
            hypernymsTEXT,
            hyponymsTEXT,
            holonymsTEXT,
            meronymsTEXT,
            troponymsTEXT,
            derivedTEXT,
            relatedTEXT,
            homophonesTEXT
        )
        """)

        # --- Create Indexes for Performance ---
        print("Creating indexes...")
        cursor.execute("CREATE INDEX IF NOT EXISTS idx_word ON entries (word)")
        cursor.execute("CREATE INDEX IF NOT EXISTS idx_pos ON entries (pos)")

        conn.commit()
        print("Database schema and indexes created successfully.")


def process_json_dump(json_dump_path, db_path):
    """
    Parses the local JSON dump and populates the SQLite database.

    Args:
        json_dump_path (str): Path to the .jsonl dump file.
        db_path (str): Path to the output SQLite database file.
    """
    conn = sqlite3.connect(db_path)
    cursor = conn.cursor()

    create_database_schema(conn)

    word_count = 0
    commit_interval = 5000  # Commit changes every 5000 words for performance

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

                    # --- Extract top-level data ---
                    etymology = word_data.get("etymology_text")
                    pron_ipa = None
                    if sounds := word_data.get("sounds"):
                        for sound in sounds:
                            if ipa := sound.get("ipa"):
                                pron_ipa = ipa
                                break

                    # --- Aggregate definitions, examples, and forms ---
                    definitions_list = []
                    examples_list = []
                    if senses := word_data.get("senses"):
                        for sense in senses:
                            definitions_list.extend(sense.get("glosses", []))
                            for example in sense.get("examples", []):
                                if example_text := example.get("text"):
                                    examples_list.append(example_text)

                    forms_list = []
                    if forms := word_data.get("forms"):
                        for form in forms:
                            form_type = form.get("form")
                            form_value = form.get("word")
                            if form_type and form_value:
                                forms_list.append({"type": form_type, "form": form_value})

                    # --- Extract each relation type into simple arrays of words ---
                    relation_mapping = {
                        "synonyms": "synonyms",
                        "antonyms": "antonyms",
                        "hypernyms": "hypernyms",
                        "hyponyms": "hyponyms",
                        "holonyms": "holonyms",
                        "meronyms": "meronyms",
                        "troponyms": "troponyms",
                        "derived": "derived",
                        "related": "related",
                        "homophones": "homophones"
                    }

                    relations_data = {}
                    for rel_type, json_key in relation_mapping.items():
                        relations_data[rel_type] = None

                        # Look for the relation data in the word_data
                        if relations := word_data.get(json_key):
                            # Extract just the words as a simple array
                            related_words = []
                            for r in relations:
                                if isinstance(r, dict) and r.get("word"):
                                    related_words.append(r["word"])
                                elif isinstance(r, str):
                                    related_words.append(r)

                            if related_words:
                                relations_data[rel_type] = json.dumps(related_words)

                    # --- Insert the row with dedicated relation columns ---
                    cursor.execute(
                        """
                        INSERT INTO entries (
                            word, pos, etymology, pronunciation_ipa,
                            definitions, examples, forms,
                            synonyms, antonyms, hypernyms,
                            hyponyms, holonyms, meronyms,
                            troponyms, derived, related,
                            homophones_json
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                        (
                            word_str, pos, etymology, pron_ipa,
                            json.dumps(definitions_list) if definitions_list else None,
                            json.dumps(examples_list) if examples_list else None,
                            json.dumps(forms_list) if forms_list else None,
                            relations_data["synonyms"], relations_data["antonyms"],
                            relations_data["hypernyms"], relations_data["hyponyms"],
                            relations_data["holonyms"], relations_data["meronyms"],
                            relations_data["troponyms"], relations_data["derived"],
                            relations_data["related"], relations_data["homophones"]
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

    # Final commit for any remaining entries
    conn.commit()
    print(f"\nExtraction complete. Total words processed: {word_count}")
    conn.close()


def main():
    """Main function to parse command-line arguments and run the process."""
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