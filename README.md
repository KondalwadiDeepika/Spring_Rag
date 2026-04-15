# Spring AI RAG Demo

This project demonstrates a simple Retrieval Augmented Generation (RAG) pipeline using Spring AI, PGVector as the vector store, and OpenAI for embedding and chat models.

[Read the full tutorial here.](https://www.sohamkamani.com/java/spring-ai-rag-application/)

Or watch the tutorial on [YouTube](https://www.youtube.com/watch?v=7TdOwFcLV5s&ab_channel=SohamKamani).

## Components

*   **Spring Boot Application:** The main application that orchestrates the RAG pipeline.
*   **PGVector:** A PostgreSQL extension used as a vector database to store and retrieve document embeddings.
*   **OpenAI Embedding Model:** Used to convert text documents and user queries into numerical vector representations.
*   **OpenAI Chat Model:** Used to generate responses based on the user's query and retrieved relevant information.
*   **DocumentLoader:** A Spring component that loads sample documents into the PGVector store on application startup.
*   **RagService:** A service that handles the RAG logic:
    *   Takes a user query.
    *   Performs a similarity search in the PGVector store to find relevant documents.
    *   Constructs a prompt for the OpenAI chat model, incorporating the original query and the retrieved document content.
    *   Returns the generated response from the OpenAI chat model.
*   **RagController:** A REST controller that exposes an endpoint (`/ai/rag`) to interact with the `RagService`.
*   **`rag-prompt.st`:** A prompt template used by the `RagService` to guide the AI model's response, ensuring it uses the provided information.

## How to Run

### Prerequisites

1.  **Java 21:** Ensure you have Java 21 installed.
2.  **Gradle:** This project uses Gradle for dependency management and building.
3.  **PostgreSQL with PGVector Extension:**
    *   Install PostgreSQL.
    *   Install the `pgvector` extension. You might need to compile it from source or use a pre-built image (e.g., `pgvector/pgvector`).
    *   Create a database (e.g., `rag_demo`). `CREATE DATABASE rag_demo;`
    *   Enable the `vector` extension in your database (after connecting to your database `\c rag_demo`):
        ```sql
        CREATE EXTENSION IF NOT EXISTS vector;
        CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

        CREATE TABLE IF NOT EXISTS vector_store (
            id uuid DEFAULT uuid_generate_v4() PRIMARY KEY,
            content text,
            embedding vector(768) 
        );

        CREATE INDEX ON vector_store USING HNSW (embedding vector_cosine_ops);
        ```
    *   This project includes a Flyway migration in `src/main/resources/db/migration/V1__init_pgvector.sql` to initialize the PGVector schema automatically on startup.
4.  **OpenAI API Key:** Obtain an API key from OpenAI.

### Configuration

1.  **`application.properties`:**
    *   Update `src/main/resources/application.properties` with your PostgreSQL connection details if they differ from the defaults.
    *   Set your OpenAI API key as an environment variable named `OPENAI_API_KEY`. Alternatively, you can directly paste it into `application.properties` (not recommended for production):
        ```properties
        spring.ai.openai.api-key=YOUR_OPENAI_API_KEY
        ```

### Build and Run

1.  **Build the application:**
    ```bash
    ./gradlew clean build
    ```
2.  **Run the application:**
    ```bash
    OPENROUTER_API_KEY=YOUR_OPENROUTER_API_KEY ./gradlew bootRun
    ```
    The `DocumentLoader` will automatically load sample documents into your PGVector database on startup.

    The Flyway migrations under `src/main/resources/db/migration` will run on startup and create the required `vector_store` table if the database is empty.

### Deployment / Production Notes

For cloud or hosted deployment, make sure your database is prepared before the application starts.

*   `spring.flyway.enabled=true` is configured in `application.properties` so Flyway runs the migration from `src/main/resources/db/migration/V1__init_pgvector.sql`.
*   The database must allow the `vector` and `uuid-ossp` extensions to be created if they do not already exist.
*   If your managed Postgres does not allow extension creation, install `vector` and `uuid-ossp` manually before deployment.
*   If Flyway is not available in your environment, create the table manually using the SQL below.

Manual initialization SQL:

```sql
CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS vector_store (
    id uuid DEFAULT uuid_generate_v4() PRIMARY KEY,
    content text,
    embedding vector(768)
);

CREATE INDEX IF NOT EXISTS vector_store_embedding_idx ON vector_store USING HNSW (embedding vector_cosine_ops);
```

Use the correct vector dimension for your model: `768` for `text-embedding-nomic-embed-text-v2-moe`, `1536` for OpenAI `text-embedding-3-small`.

### Test the RAG Endpoint

Once the application is running, you can access the RAG endpoint:

```bash
curl "http://localhost:8080/ai/rag?message=What is RAG?"
```

You should receive a response generated by the OpenAI chat model, augmented with information retrieved from your PGVector store.