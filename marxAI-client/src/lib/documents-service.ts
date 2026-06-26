import { apiClient } from "@/lib/api-client";
import type { DocumentResponse } from "@/types/documents";

/** Wraps the {@code /api/docs} endpoints exposed by DocumentController. */
export const documentsService = {
  /** Fetches all documents for the authenticated user, newest-first. */
  listDocuments(): Promise<DocumentResponse[]> {
    return apiClient.get<DocumentResponse[]>("/api/docs").then((r) => r.data);
  },

  /**
   * Uploads a file as a new document in the specified knowledge-base category.
   * The returned document will be in {@code PROCESSING} status until ingestion completes.
   */
  uploadDocument(file: File, docType: string): Promise<DocumentResponse> {
    const form = new FormData();
    form.append("file", file);
    form.append("docType", docType);
    return apiClient
      .post<DocumentResponse>("/api/docs/upload", form, {
        headers: { "Content-Type": "multipart/form-data" },
      })
      .then((r) => r.data);
  },

  /**
   * Deletes a document and all of its associated vectors and storage objects.
   * Returns void on success (HTTP 204).
   */
  deleteDocument(id: string): Promise<void> {
    return apiClient.delete(`/api/docs/${id}`).then(() => undefined);
  },
};
