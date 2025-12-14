"use client";

import { useState } from "react";
import { useMutation, useQuery } from "@apollo/client";
import {
  GET_DOCUMENTS,
  GET_CONFLICTS,
  INGEST_DOCUMENT,
  ANALYZE_CONFLICTS,
} from "@/lib/graphql-queries";

export default function Home() {
  const [name, setName] = useState("");
  const [content, setContent] = useState("");
  const [documentType, setDocumentType] = useState("CONTRACT");

  const { data: documentsData, loading: docsLoading, refetch: refetchDocs } = useQuery(GET_DOCUMENTS);
  const { data: conflictsData, refetch: refetchConflicts } = useQuery(GET_CONFLICTS);

  const [analyzeConflicts, { loading: analyzing }] = useMutation(ANALYZE_CONFLICTS, {
    onCompleted: () => {
      refetchConflicts();
    },
  });

  const [ingestDocument, { loading: ingesting }] = useMutation(INGEST_DOCUMENT, {
    onCompleted: () => {
      refetchDocs();
      refetchConflicts();
      setName("");
      setContent("");
    },
  });

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim() || !content.trim()) return;
    await ingestDocument({
      variables: { input: { name, content, documentType } },
    });
  };

  const handleAnalyze = () => {
    const documentIds = documentsData?.documents?.map((d: any) => d.id) || [];
    if (documentIds.length >= 2) {
      analyzeConflicts({ variables: { documentIds } });
    }
  };

  const docCount = documentsData?.documents?.length || 0;
  const conflictCount = conflictsData?.conflicts?.length || 0;

  return (
    <main className="min-h-screen py-8 px-4">
      <div className="max-w-3xl mx-auto">
        {/* Header */}
        <header className="mb-8">
          <h1 className="text-2xl font-bold text-gray-900">ConflictGuard</h1>
          <p className="text-gray-500 mt-1">Document conflict detection using AI</p>
        </header>

        {/* Upload Form */}
        <div className="card mb-6">
          <h2 className="section-title mb-4">Upload Document</h2>
          <form onSubmit={handleSubmit} className="space-y-4">
            <div className="flex gap-3">
              <input
                type="text"
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder="Document name"
                className="flex-1"
              />
              <select
                value={documentType}
                onChange={(e) => setDocumentType(e.target.value)}
                className="w-40"
              >
                <option value="CONTRACT">Contract</option>
                <option value="TERMS_AND_CONDITIONS">Terms</option>
                <option value="INTERNAL_DIRECTIVE">Directive</option>
                <option value="OTHER">Other</option>
              </select>
            </div>
            <textarea
              value={content}
              onChange={(e) => setContent(e.target.value)}
              placeholder="Paste document content here..."
              rows={5}
              className="w-full resize-none"
            />
            <button
              type="submit"
              disabled={ingesting || !name.trim() || !content.trim()}
              className="btn btn-primary"
            >
              {ingesting ? "Processing..." : "Upload & Extract Entities"}
            </button>
          </form>
        </div>

        {/* Documents */}
        <div className="card mb-6">
          <div className="section-header">
            <h2 className="section-title">
              Documents <span className="section-count">({docCount})</span>
            </h2>
            <button
              onClick={handleAnalyze}
              disabled={analyzing || docCount < 2}
              className="btn btn-success text-sm"
            >
              {analyzing ? "Analyzing..." : "Analyze Conflicts"}
            </button>
          </div>

          {docsLoading ? (
            <div className="empty-state">Loading...</div>
          ) : docCount === 0 ? (
            <div className="empty-state">
              No documents yet. Upload at least 2 documents to analyze conflicts.
            </div>
          ) : (
            <div className="space-y-3">
              {documentsData?.documents?.map((doc: any) => (
                <div key={doc.id} className="doc-item">
                  <div className="flex items-start justify-between">
                    <div>
                      <h3 className="font-medium text-gray-900">{doc.name}</h3>
                      <p className="text-sm text-gray-500 mt-0.5">
                        {doc.documentType.replace(/_/g, " ")} · {doc.entities?.length || 0} entities
                      </p>
                    </div>
                  </div>
                  {doc.entities?.length > 0 && (
                    <div className="mt-3 flex flex-wrap gap-1.5">
                      {doc.entities.slice(0, 5).map((e: any) => (
                        <span key={e.id} className="entity-tag">
                          {e.name}
                        </span>
                      ))}
                      {doc.entities.length > 5 && (
                        <span className="entity-tag text-gray-400">
                          +{doc.entities.length - 5} more
                        </span>
                      )}
                    </div>
                  )}
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Conflicts */}
        <div className="card">
          <div className="section-header">
            <h2 className="section-title">
              Conflicts <span className="section-count">({conflictCount})</span>
            </h2>
          </div>

          {conflictCount === 0 ? (
            <div className="empty-state">
              No conflicts detected. Upload documents and click "Analyze Conflicts".
            </div>
          ) : (
            <div className="space-y-3">
              {conflictsData?.conflicts?.map((conflict: any) => (
                <div
                  key={conflict.id}
                  className={`p-4 rounded-lg severity-${conflict.severity?.toLowerCase()}`}
                >
                  <div className="flex items-center gap-2 mb-2">
                    <span className={`badge badge-${conflict.severity?.toLowerCase()}`}>
                      {conflict.severity}
                    </span>
                  </div>
                  <p className="font-medium text-gray-900">{conflict.description}</p>
                  <p className="text-sm text-gray-600 mt-2">{conflict.reasoning}</p>
                  {conflict.legalPrinciple && (
                    <p className="text-xs text-gray-500 mt-2">
                      <span className="font-medium">Legal principle:</span> {conflict.legalPrinciple}
                    </p>
                  )}
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </main>
  );
}
