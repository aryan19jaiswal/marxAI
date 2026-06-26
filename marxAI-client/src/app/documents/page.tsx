"use client";

import { useCallback, useEffect, useState } from "react";
import { useDropzone } from "react-dropzone";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { FileText, Loader2, Trash2, Upload, X } from "lucide-react";

import { documentsService } from "@/lib/documents-service";
import { getApiErrorMessage } from "@/lib/api-client";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";
import type { DocumentResponse } from "@/types/documents";

// ---------------------------------------------------------------------------
// Types
// ---------------------------------------------------------------------------

type Toast = { type: "success" | "error"; message: string };

const DOC_TYPES = ["DSA", "SYSTEM_DESIGN", "RESUME"] as const;
type DocType = (typeof DOC_TYPES)[number];

const DOC_TYPE_LABELS: Record<DocType, string> = {
  DSA: "DSA",
  SYSTEM_DESIGN: "System Design",
  RESUME: "Resume",
};

// ---------------------------------------------------------------------------
// Status badge helper
// ---------------------------------------------------------------------------

function StatusBadge({ status }: { status: DocumentResponse["status"] }) {
  const map = {
    PROCESSING: "bg-yellow-100 text-yellow-800 dark:bg-yellow-900/40 dark:text-yellow-300",
    READY: "bg-green-100 text-green-800 dark:bg-green-900/40 dark:text-green-300",
    FAILED: "bg-red-100 text-red-800 dark:bg-red-900/40 dark:text-red-300",
  } as const;

  return (
    <span
      className={cn(
        "inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-xs font-medium",
        map[status],
      )}
    >
      {status === "PROCESSING" && <Loader2 className="size-3 animate-spin" />}
      {status}
    </span>
  );
}

// ---------------------------------------------------------------------------
// Page
// ---------------------------------------------------------------------------

export default function DocumentsPage() {
  const queryClient = useQueryClient();

  // Toast state — auto-dismissed after 4 s.
  const [toast, setToast] = useState<Toast | null>(null);
  useEffect(() => {
    if (!toast) return;
    const id = setTimeout(() => setToast(null), 4000);
    return () => clearTimeout(id);
  }, [toast]);

  // Upload form state.
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [docType, setDocType] = useState<DocType>("DSA");

  // Delete confirmation modal state.
  const [documentToDelete, setDocumentToDelete] = useState<DocumentResponse | null>(null);

  // -------------------------------------------------------------------------
  // Fetch documents — poll every 3 s while any document is still PROCESSING.
  // -------------------------------------------------------------------------
  const { data: documents = [], isLoading } = useQuery({
    queryKey: ["documents"],
    queryFn: documentsService.listDocuments,
    refetchInterval: (query) => {
      const docs = query.state.data ?? [];
      return docs.some((d) => d.status === "PROCESSING") ? 3000 : false;
    },
  });

  // -------------------------------------------------------------------------
  // Upload mutation
  // -------------------------------------------------------------------------
  const uploadMutation = useMutation({
    mutationFn: ({ file, type }: { file: File; type: DocType }) =>
      documentsService.uploadDocument(file, type),
    onSuccess: (created) => {
      queryClient.invalidateQueries({ queryKey: ["documents"] });
      setSelectedFile(null);
      setToast({
        type: "success",
        message: `"${created.filename}" uploaded — processing in background.`,
      });
    },
    onError: (err) => {
      setToast({
        type: "error",
        message: getApiErrorMessage(err, "Upload failed. Please try again."),
      });
    },
  });

  // -------------------------------------------------------------------------
  // Delete mutation
  // -------------------------------------------------------------------------
  const deleteMutation = useMutation({
    mutationFn: (id: string) => documentsService.deleteDocument(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["documents"] });
      setDocumentToDelete(null);
      setToast({ type: "success", message: "Document deleted." });
    },
    onError: (err) => {
      setDocumentToDelete(null);
      setToast({
        type: "error",
        message: getApiErrorMessage(err, "Delete failed. Please try again."),
      });
    },
  });

  // -------------------------------------------------------------------------
  // Dropzone
  // -------------------------------------------------------------------------
  const onDrop = useCallback((accepted: File[]) => {
    if (accepted[0]) setSelectedFile(accepted[0]);
  }, []);

  const { getRootProps, getInputProps, isDragActive } = useDropzone({
    onDrop,
    accept: {
      "application/pdf": [".pdf"],
      "text/plain": [".txt"],
      "text/markdown": [".md"],
    },
    maxFiles: 1,
    multiple: false,
  });

  const handleUpload = () => {
    if (!selectedFile) return;
    uploadMutation.mutate({ file: selectedFile, type: docType });
  };

  // -------------------------------------------------------------------------
  // Render
  // -------------------------------------------------------------------------
  return (
    <div className="flex flex-col gap-8">
      {/* Page title */}
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">Documents</h1>
        <p className="text-muted-foreground">
          Upload your notes, cheat-sheets, and resumes to power the AI agents.
        </p>
      </div>

      {/* ------------------------------------------------------------------ */}
      {/* Upload card                                                          */}
      {/* ------------------------------------------------------------------ */}
      <div className="rounded-xl border border-border bg-card p-6 shadow-sm">
        <h2 className="mb-4 text-base font-semibold">Upload a document</h2>

        {/* Dropzone */}
        <div
          {...getRootProps()}
          className={cn(
            "flex cursor-pointer flex-col items-center justify-center gap-2 rounded-lg border-2 border-dashed p-10 text-center transition-colors",
            isDragActive
              ? "border-primary bg-primary/5"
              : "border-border bg-muted/30 hover:border-primary/60 hover:bg-primary/5",
          )}
        >
          <input {...getInputProps()} />
          <Upload className="size-8 text-muted-foreground" />
          {isDragActive ? (
            <p className="text-sm font-medium text-primary">Drop the file here…</p>
          ) : (
            <>
              <p className="text-sm font-medium">
                Drag &amp; drop, or{" "}
                <span className="text-primary underline underline-offset-2">browse</span>
              </p>
              <p className="text-xs text-muted-foreground">PDF, TXT, or Markdown · max 50 MB</p>
            </>
          )}
        </div>

        {/* Selected file chip */}
        {selectedFile && (
          <div className="mt-3 flex items-center gap-2 rounded-lg border border-border bg-background px-3 py-2 text-sm">
            <FileText className="size-4 shrink-0 text-muted-foreground" />
            <span className="min-w-0 flex-1 truncate">{selectedFile.name}</span>
            <button
              onClick={() => setSelectedFile(null)}
              className="shrink-0 text-muted-foreground hover:text-foreground"
              aria-label="Remove file"
            >
              <X className="size-4" />
            </button>
          </div>
        )}

        {/* Doc type selector + submit */}
        <div className="mt-4 flex flex-wrap items-center gap-3">
          <div className="flex items-center gap-2">
            <label
              htmlFor="doc-type"
              className="text-sm font-medium text-muted-foreground"
            >
              Category
            </label>
            <select
              id="doc-type"
              value={docType}
              onChange={(e) => setDocType(e.target.value as DocType)}
              className="rounded-lg border border-border bg-background px-2 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-ring"
            >
              {DOC_TYPES.map((t) => (
                <option key={t} value={t}>
                  {DOC_TYPE_LABELS[t]}
                </option>
              ))}
            </select>
          </div>

          <Button
            onClick={handleUpload}
            disabled={!selectedFile || uploadMutation.isPending}
            className="ml-auto"
          >
            {uploadMutation.isPending ? (
              <>
                <Loader2 className="size-4 animate-spin" />
                Uploading…
              </>
            ) : (
              <>
                <Upload className="size-4" />
                Upload
              </>
            )}
          </Button>
        </div>
      </div>

      {/* ------------------------------------------------------------------ */}
      {/* Document list table                                                  */}
      {/* ------------------------------------------------------------------ */}
      <div className="rounded-xl border border-border bg-card shadow-sm">
        <div className="flex items-center justify-between border-b border-border px-6 py-4">
          <h2 className="text-base font-semibold">Your knowledge base</h2>
          <span className="text-sm text-muted-foreground">
            {documents.length} document{documents.length !== 1 ? "s" : ""}
          </span>
        </div>

        {isLoading ? (
          <div className="flex items-center justify-center py-16 text-muted-foreground">
            <Loader2 className="mr-2 size-5 animate-spin" />
            Loading…
          </div>
        ) : documents.length === 0 ? (
          <div className="flex flex-col items-center justify-center gap-2 py-16 text-center text-muted-foreground">
            <FileText className="size-10 opacity-30" />
            <p className="text-sm">No documents yet. Upload one above to get started.</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-border text-left text-xs font-medium uppercase tracking-wider text-muted-foreground">
                  <th className="px-6 py-3">Filename</th>
                  <th className="px-6 py-3">Category</th>
                  <th className="px-6 py-3">Status</th>
                  <th className="px-6 py-3">Uploaded</th>
                  <th className="px-6 py-3 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border">
                {documents.map((doc) => (
                  <tr key={doc.id} className="hover:bg-muted/30 transition-colors">
                    <td className="flex max-w-xs items-center gap-2 px-6 py-3">
                      <FileText className="size-4 shrink-0 text-muted-foreground" />
                      <span className="truncate font-medium">{doc.filename}</span>
                    </td>
                    <td className="px-6 py-3 text-muted-foreground">
                      {DOC_TYPE_LABELS[doc.docType as DocType] ?? doc.docType}
                    </td>
                    <td className="px-6 py-3">
                      <StatusBadge status={doc.status} />
                    </td>
                    <td className="px-6 py-3 text-muted-foreground">
                      {new Date(doc.uploadedAt).toLocaleDateString(undefined, {
                        year: "numeric",
                        month: "short",
                        day: "numeric",
                      })}
                    </td>
                    <td className="px-6 py-3 text-right">
                      <Button
                        variant="ghost"
                        size="icon-sm"
                        onClick={() => setDocumentToDelete(doc)}
                        aria-label={`Delete ${doc.filename}`}
                        className="text-muted-foreground hover:text-destructive"
                      >
                        <Trash2 className="size-4" />
                      </Button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* ------------------------------------------------------------------ */}
      {/* Delete confirmation modal                                            */}
      {/* ------------------------------------------------------------------ */}
      {documentToDelete && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/50"
          onClick={() => setDocumentToDelete(null)}
        >
          <div
            className="w-full max-w-md rounded-xl border border-border bg-background p-6 shadow-2xl"
            onClick={(e) => e.stopPropagation()}
          >
            <h2 className="text-lg font-semibold">Delete document?</h2>
            <p className="mt-2 text-sm text-muted-foreground">
              <span className="font-medium text-foreground">
                &ldquo;{documentToDelete.filename}&rdquo;
              </span>{" "}
              and all its embeddings will be permanently removed. This cannot be undone.
            </p>

            <div className="mt-6 flex justify-end gap-3">
              <Button
                variant="outline"
                onClick={() => setDocumentToDelete(null)}
                disabled={deleteMutation.isPending}
              >
                Cancel
              </Button>
              <Button
                variant="destructive"
                onClick={() => deleteMutation.mutate(documentToDelete.id)}
                disabled={deleteMutation.isPending}
              >
                {deleteMutation.isPending ? (
                  <>
                    <Loader2 className="size-4 animate-spin" />
                    Deleting…
                  </>
                ) : (
                  "Delete"
                )}
              </Button>
            </div>
          </div>
        </div>
      )}

      {/* ------------------------------------------------------------------ */}
      {/* Toast notification                                                   */}
      {/* ------------------------------------------------------------------ */}
      {toast && (
        <div
          role="status"
          aria-live="polite"
          className={cn(
            "fixed bottom-6 right-6 z-50 flex max-w-sm items-start gap-3 rounded-xl border px-4 py-3 shadow-lg transition-opacity",
            toast.type === "success"
              ? "border-green-200 bg-green-50 text-green-900 dark:border-green-800 dark:bg-green-950 dark:text-green-100"
              : "border-red-200 bg-red-50 text-red-900 dark:border-red-800 dark:bg-red-950 dark:text-red-100",
          )}
        >
          <p className="flex-1 text-sm">{toast.message}</p>
          <button
            onClick={() => setToast(null)}
            className="shrink-0 opacity-60 hover:opacity-100"
            aria-label="Dismiss notification"
          >
            <X className="size-4" />
          </button>
        </div>
      )}
    </div>
  );
}
