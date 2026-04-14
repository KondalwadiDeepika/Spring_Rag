import http.server
import socketserver

# Define the port the server will run on
PORT = 8000

class LoggingRequestHandler(http.server.BaseHTTPRequestHandler):
    """
    Custom request handler that logs requests and returns a 400 error.
    This version correctly handles both 'Content-Length' and 'Transfer-Encoding: chunked'.
    """
    def handle_request(self):
        """
        Logs the entire request content and sends a 400 response.
        """
        print("--- INCOMING REQUEST ---")
        
        # Log request line
        print(f"\nRequest Line:\n{self.requestline}")
        
        # Log headers
        print(f"\nHeaders:\n{self.headers}")
        
        body = b''
        # Check for 'Transfer-Encoding: chunked'
        if self.headers.get('Transfer-Encoding', '').lower() == 'chunked':
            print("\nReading chunked body...")
            while True:
                # Read the line containing the chunk size
                chunk_size_line = self.rfile.readline().strip()
                if not chunk_size_line:
                    continue
                # The chunk size is hex, so convert to int
                chunk_size = int(chunk_size_line, 16)
                if chunk_size == 0:
                    # End of chunks
                    self.rfile.readline() # Consume the final blank line
                    break
                # Read the chunk data
                body += self.rfile.read(chunk_size)
                # Consume the newline character that follows the chunk
                self.rfile.readline()
        # Fallback to 'Content-Length' if not chunked
        else:
            content_length = int(self.headers.get('Content-Length', 0))
            if content_length > 0:
                print(f"\nReading body with Content-Length: {content_length}...")
                body = self.rfile.read(content_length)

        if body:
            print("\nBody:")
            try:
                # Attempt to decode for prettier printing
                print(body.decode('utf-8'))
            except UnicodeDecodeError:
                print(body) # Print raw bytes if not UTF-8
        
        print("\n--- END OF REQUEST ---\n")
        
        # Send a 400 Bad Request response
        self.send_response(400)
        self.send_header('Content-type', 'text/plain')
        self.end_headers()
        self.wfile.write(b"400 Bad Request")

    # Handle all common HTTP methods by routing them to our custom handler
    def do_GET(self):
        self.handle_request()

    def do_POST(self):
        self.handle_request()
        
    def do_PUT(self):
        self.handle_request()

    def do_DELETE(self):
        self.handle_request()
        
    def do_PATCH(self):
        self.handle_request()

# --- Main execution ---
if __name__ == "__main__":
    with socketserver.TCPServer(("", PORT), LoggingRequestHandler) as httpd:
        print(f"🖥️  Starting logging server on http://localhost:{PORT}")
        print("   This version supports chunked transfer encoding.")
        try:
            httpd.serve_forever()
        except KeyboardInterrupt:
            print("\n🛑 Server is shutting down.")
            httpd.server_close()