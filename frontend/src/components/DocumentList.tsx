"use client";

interface Document {
  id: string;
  name: string;
  documentType: string;
  createdAt: string;
  entities: Array<{
    id: string;
    name: string;
    entityType: string;
    value: string;
  }>;
}

interface DocumentListProps {
  documents: Document[];
  loading: boolean;
  selectedIds: string[];
  onToggleSelect: (id: string) => void;
}

const typeColors: Record<string, string> = {
  CONTRACT: "bg-blue-500/20 text-blue-400 border-blue-500/50",
  TERMS_AND_CONDITIONS: "bg-purple-500/20 text-purple-400 border-purple-500/50",
  INTERNAL_DIRECTIVE: "bg-orange-500/20 text-orange-400 border-orange-500/50",
  REGULATION: "bg-green-500/20 text-green-400 border-green-500/50",
  OTHER: "bg-gray-500/20 text-gray-400 border-gray-500/50",
};

export default function DocumentList({ documents, loading, selectedIds, onToggleSelect }: DocumentListProps) {
  if (loading) {
    return (
      <div className="glass rounded-2xl p-8 text-center">
        <div className="animate-spin w-8 h-8 border-2 border-primary-500 border-t-transparent rounded-full mx-auto mb-4" />
        <p className="text-gray-400">Loading documents...</p>
      </div>
    );
  }

  if (documents.length === 0) {
    return (
      <div className="glass rounded-2xl p-8 text-center">
        <div className="w-16 h-16 rounded-full bg-white/5 flex items-center justify-center mx-auto mb-4">
          <svg className="w-8 h-8 text-gray-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
          </svg>
        </div>
        <p className="text-gray-400">No documents uploaded yet</p>
        <p className="text-sm text-gray-500 mt-1">Upload a document to get started</p>
      </div>
    );
  }

  return (
    <div className="glass rounded-2xl p-6">
      <h2 className="text-lg font-semibold mb-4 flex items-center gap-2">
        <svg className="w-5 h-5 text-primary-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
        </svg>
        Documents ({documents.length})
      </h2>

      <div className="space-y-3">
        {documents.map((doc) => (
          <div
            key={doc.id}
            onClick={() => onToggleSelect(doc.id)}
            className={`p-4 rounded-xl border cursor-pointer transition-all ${
              selectedIds.includes(doc.id)
                ? "bg-primary-500/10 border-primary-500/50"
                : "bg-white/5 border-white/10 hover:border-white/20"
            }`}
          >
            <div className="flex items-start justify-between gap-4">
              <div className="flex-1 min-w-0">
                <div className="flex items-center gap-2 mb-2">
                  <input
                    type="checkbox"
                    checked={selectedIds.includes(doc.id)}
                    onChange={() => {}}
                    className="w-4 h-4 rounded border-white/20 bg-white/5 text-primary-500 focus:ring-primary-500"
                  />
                  <h3 className="font-medium truncate">{doc.name}</h3>
                  <span className={`text-xs px-2 py-0.5 rounded-full border ${typeColors[doc.documentType] || typeColors.OTHER}`}>
                    {doc.documentType.replace(/_/g, " ")}
                  </span>
                </div>

                {doc.entities.length > 0 && (
                  <div className="flex flex-wrap gap-1.5 mt-2">
                    {doc.entities.slice(0, 5).map((entity) => (
                      <span
                        key={entity.id}
                        className="text-xs px-2 py-1 rounded-lg bg-white/5 text-gray-300"
                        title={`${entity.entityType}: ${entity.value}`}
                      >
                        {entity.name}
                      </span>
                    ))}
                    {doc.entities.length > 5 && (
                      <span className="text-xs px-2 py-1 rounded-lg bg-white/5 text-gray-500">
                        +{doc.entities.length - 5} more
                      </span>
                    )}
                  </div>
                )}
              </div>

              <div className="text-right text-xs text-gray-500">
                {doc.entities.length} entities
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
