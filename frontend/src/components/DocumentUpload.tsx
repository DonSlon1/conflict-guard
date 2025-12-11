"use client";

import { useState } from "react";

interface DocumentUploadProps {
  onUpload: (name: string, content: string, documentType: string) => Promise<void>;
  loading: boolean;
}

const documentTypes = [
  { value: "CONTRACT", label: "Contract" },
  { value: "TERMS_AND_CONDITIONS", label: "Terms & Conditions" },
  { value: "INTERNAL_DIRECTIVE", label: "Internal Directive" },
  { value: "REGULATION", label: "Regulation" },
  { value: "OTHER", label: "Other" },
];

export default function DocumentUpload({ onUpload, loading }: DocumentUploadProps) {
  const [name, setName] = useState("");
  const [content, setContent] = useState("");
  const [documentType, setDocumentType] = useState("CONTRACT");

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim() || !content.trim()) return;

    await onUpload(name, content, documentType);
    setName("");
    setContent("");
  };

  return (
    <div className="glass rounded-2xl p-6">
      <h2 className="text-lg font-semibold mb-4 flex items-center gap-2">
        <svg className="w-5 h-5 text-primary-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12" />
        </svg>
        Upload Document
      </h2>

      <form onSubmit={handleSubmit} className="space-y-4">
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <label className="block text-sm text-gray-400 mb-1">Document Name</label>
            <input
              type="text"
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="e.g., Smlouva 2024"
              className="w-full px-4 py-2.5 rounded-xl bg-white/5 border border-white/10 focus:border-primary-500 focus:outline-none focus:ring-1 focus:ring-primary-500 transition-all"
            />
          </div>
          <div>
            <label className="block text-sm text-gray-400 mb-1">Document Type</label>
            <select
              value={documentType}
              onChange={(e) => setDocumentType(e.target.value)}
              className="w-full px-4 py-2.5 rounded-xl bg-white/5 border border-white/10 focus:border-primary-500 focus:outline-none focus:ring-1 focus:ring-primary-500 transition-all"
            >
              {documentTypes.map((type) => (
                <option key={type.value} value={type.value} className="bg-slate-800">
                  {type.label}
                </option>
              ))}
            </select>
          </div>
        </div>

        <div>
          <label className="block text-sm text-gray-400 mb-1">Document Content</label>
          <textarea
            value={content}
            onChange={(e) => setContent(e.target.value)}
            placeholder="Paste the document text here..."
            rows={5}
            className="w-full px-4 py-3 rounded-xl bg-white/5 border border-white/10 focus:border-primary-500 focus:outline-none focus:ring-1 focus:ring-primary-500 transition-all resize-none"
          />
        </div>

        <button
          type="submit"
          disabled={loading || !name.trim() || !content.trim()}
          className="w-full py-3 rounded-xl bg-gradient-to-r from-primary-500 to-accent-500 font-medium hover:shadow-lg hover:shadow-primary-500/25 transition-all disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2"
        >
          {loading ? (
            <>
              <svg className="animate-spin h-5 w-5" viewBox="0 0 24 24">
                <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" fill="none" />
                <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
              </svg>
              Processing with AI...
            </>
          ) : (
            <>
              <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-8l-4-4m0 0L8 8m4-4v12" />
              </svg>
              Upload & Extract Entities
            </>
          )}
        </button>
      </form>
    </div>
  );
}
