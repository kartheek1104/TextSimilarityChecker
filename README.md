# Text Similarity Checker

This project is a Java-based desktop application for computing the similarity between two input texts. It uses natural language processing techniques including tokenization, synonym expansion, cosine similarity, and fuzzy string matching to determine how similar two pieces of text are.

## Features

- Lucene-based tokenization for accurate word analysis
- Synonym expansion from a custom text file
- Cosine similarity computation based on token frequency vectors
- Hybrid fuzzy similarity using both Jaro-Winkler and Levenshtein distance
- Token-level similarity mapping and matched phrase highlighting
- Support for `.txt` and `.pdf` inputs
- Export functionality for results (TXT and optional PDF)
- GUI built using Java Swing with clean, user-friendly layout



## Requirements

- Java 8 or later
- Apache Lucene Core
- Apache Commons Text

Make sure the required libraries are available in your classpath when compiling and running the program.

## How to Run

1. Clone the repository:
```bash
git clone https://github.com/kartheek1104/TextSimilarityChecker.git
cd TextSimilarityChecker
```


2. Compile the Java source files:
```bash
javac -cp "lib/" -d bin src/.java
```


3. Run the application:

```bash

java -cp "bin;lib/*" Main
```



> Note: Adjust the classpath syntax depending on your operating system.

## Input

- You can place test documents in the `input/` folder.
- The synonym list used for expansion must be named `synonyms.txt` and should follow this format:

quick,fast,swift
car,automobile,vehicle


## Output

- Similarity results can be exported as `.txt` files into the `output/` folder.
- PDF export allows selecting the destination manually at runtime.
