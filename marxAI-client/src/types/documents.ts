/**
 * Mirrors DocumentResponse from com.marxAI.model.dto.DocumentResponse.
 * Keep in sync with the backend DTO.
 */
export interface DocumentResponse {
  id: string;
  filename: string;
  /** Knowledge-base category: DSA | SYSTEM_DESIGN | RESUME */
  docType: string;
  /** Ingestion lifecycle state: PROCESSING | READY | FAILED */
  status: "PROCESSING" | "READY" | "FAILED";
  uploadedAt: string;
}
