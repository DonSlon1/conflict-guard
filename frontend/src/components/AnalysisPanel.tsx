"use client";

import { useState } from "react";
import { useMutation } from "@apollo/client";
import { ANALYZE_CONFLICTS } from "@/lib/graphql-queries";

interface Document {
  id: string;
  name: string;
  documentType: string;
}

interface AnalysisPanelProps {
  documents: Document[];
  selectedIds: string[];
  onToggleSelect: (id: string) => void;
  onAnalysisComplete: () => void;
}

export default function AnalysisPanel({
  documents,
  selectedIds,
  onToggleSelect,
  onAnalysisComplete,
}: AnalysisPanelProps) {
  const [analysisResult, setAnalysisResult] = useState<any>(null);

  const [analyzeConflicts, { loading }] = useMutation(ANALYZE_CONFLICTS, {
    onCompleted: (data) => {
      setAnalysisResult(data.analyzeConflicts);
      onAnalysisComplete();
    },
  });

  const handleAnalyze = () => {
    if (selectedIds.length < 2) return;
    analyzeConflicts({ variables: { documentIds: selectedIds } });
  };

  return (
    <div className="space-y-6">
      {/* Document Selection */}
      <div className="glass rounded-2xl p-6">
        <h2 className="text-lg font-semibold mb-4 flex items-center gap-2">
          <svg className="w-5 h-5 text-primary-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-6 9l2 2 4-4" />
          </svg>
          Select Documents to Analyze
        </h2>

        {documents.length === 0 ? (
          <p className="text-gray-400 text-sm">No documents available. Upload some first.</p>
        ) : (
          <>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 mb-4">
              {documents.map((doc) => (
                <label
                  key={doc.id}
                  className={`flex items-center gap-3 p-3 rounded-xl cursor-pointer transition-all ${
                    selectedIds.includes(doc.id)
                      ? "bg-primary-500/10 border border-primary-500/50"
                      : "bg-white/5 border border-white/10 hover:border-white/20"
                  }`}
                >
                  <input
                    type="checkbox"
                    checked={selectedIds.includes(doc.id)}
                    onChange={() => onToggleSelect(doc.id)}
                    className="w-4 h-4 rounded border-white/20 bg-white/5 text-primary-500 focus:ring-primary-500"
                  />
                  <div className="min-w-0">
                    <div className="font-medium truncate">{doc.name}</div>
                    <div className="text-xs text-gray-500">{doc.documentType.replace(/_/g, " ")}</div>
                  </div>
                </label>
              ))}
            </div>

            <button
              onClick={handleAnalyze}
              disabled={loading || selectedIds.length < 2}
              className="w-full py-3 rounded-xl bg-gradient-to-r from-red-500 to-orange-500 font-medium hover:shadow-lg hover:shadow-red-500/25 transition-all disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2"
            >
              {loading ? (
                <>
                  <svg className="animate-spin h-5 w-5" viewBox="0 0 24 24">
                    <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" fill="none" />
                    <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
                  </svg>
                  AI Analyzing Conflicts...
                </>
              ) : (
                <>
                  <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.548.547A3.374 3.374 0 0014 18.469V19a2 2 0 11-4 0v-.531c0-.895-.356-1.754-.988-2.386l-.548-.547z" />
                  </svg>
                  Analyze {selectedIds.length} Documents for Conflicts
                </>
              )}
            </button>

            {selectedIds.length < 2 && (
              <p className="text-xs text-gray-500 text-center mt-2">
                Select at least 2 documents to analyze
              </p>
            )}
          </>
        )}
      </div>

      {/* Analysis Results */}
      {analysisResult && (
        <div className="glass rounded-2xl p-6">
          <h2 className="text-lg font-semibold mb-4 flex items-center gap-2">
            <svg className="w-5 h-5 text-accent-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 17v-2m3 2v-4m3 4v-6m2 10H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
            </svg>
            Analysis Results
          </h2>

          <div className="mb-4 p-4 rounded-xl bg-white/5 border border-white/10">
            <p className="text-sm text-gray-300">{analysisResult.summary}</p>
            <p className="text-xs text-gray-500 mt-2">
              Analyzed at: {new Date(analysisResult.analyzedAt).toLocaleString()}
            </p>
          </div>

          {analysisResult.conflicts.length === 0 ? (
            <div className="text-center py-6">
              <div className="w-12 h-12 rounded-full bg-green-500/10 flex items-center justify-center mx-auto mb-3">
                <svg className="w-6 h-6 text-green-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                </svg>
              </div>
              <p className="text-green-400 font-medium">No conflicts detected!</p>
            </div>
          ) : (
            <div className="space-y-3">
              {analysisResult.conflicts.map((conflict: any) => (
                <div
                  key={conflict.id}
                  className={`p-4 rounded-xl border ${
                    conflict.severity === "CRITICAL"
                      ? "severity-critical"
                      : conflict.severity === "HIGH"
                      ? "severity-high"
                      : conflict.severity === "MEDIUM"
                      ? "severity-medium"
                      : "severity-low"
                  }`}
                >
                  <div className="flex items-start justify-between gap-3 mb-2">
                    <h3 className="font-medium text-sm">{conflict.description}</h3>
                    <span className="text-xs font-medium px-2 py-0.5 rounded-full bg-current/20">
                      {conflict.severity}
                    </span>
                  </div>
                  <p className="text-xs opacity-80">{conflict.reasoning}</p>
                  {conflict.legalPrinciple && (
                    <p className="text-xs mt-2 opacity-60">
                      Legal Principle: <span className="font-medium">{conflict.legalPrinciple}</span>
                    </p>
                  )}
                </div>
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  );
}
