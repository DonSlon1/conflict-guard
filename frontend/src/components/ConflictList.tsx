"use client";

interface Conflict {
  id: string;
  description: string;
  severity: string;
  reasoning: string;
  legalPrinciple: string | null;
  entities: Array<{
    id: string;
    name: string;
    value: string;
    entityType: string;
  }>;
}

interface ConflictListProps {
  conflicts: Conflict[];
  loading: boolean;
}

const severityOrder = ["CRITICAL", "HIGH", "MEDIUM", "LOW"];

export default function ConflictList({ conflicts, loading }: ConflictListProps) {
  if (loading) {
    return (
      <div className="glass rounded-2xl p-8 text-center">
        <div className="animate-spin w-8 h-8 border-2 border-red-500 border-t-transparent rounded-full mx-auto mb-4" />
        <p className="text-gray-400">Loading conflicts...</p>
      </div>
    );
  }

  if (conflicts.length === 0) {
    return (
      <div className="glass rounded-2xl p-8 text-center">
        <div className="w-16 h-16 rounded-full bg-green-500/10 flex items-center justify-center mx-auto mb-4">
          <svg className="w-8 h-8 text-green-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
        </div>
        <p className="text-gray-400">No conflicts detected</p>
        <p className="text-sm text-gray-500 mt-1">Your documents appear to be consistent</p>
      </div>
    );
  }

  const sortedConflicts = [...conflicts].sort(
    (a, b) => severityOrder.indexOf(a.severity) - severityOrder.indexOf(b.severity)
  );

  return (
    <div className="glass rounded-2xl p-6">
      <h2 className="text-lg font-semibold mb-4 flex items-center gap-2">
        <svg className="w-5 h-5 text-red-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
        </svg>
        Conflicts ({conflicts.length})
      </h2>

      <div className="space-y-4">
        {sortedConflicts.map((conflict) => (
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
            <div className="flex items-start justify-between gap-4 mb-3">
              <h3 className="font-medium">{conflict.description}</h3>
              <span className="flex-shrink-0 text-xs font-medium px-2 py-1 rounded-full bg-current/20">
                {conflict.severity}
              </span>
            </div>

            <p className="text-sm opacity-80 mb-3">{conflict.reasoning}</p>

            {conflict.legalPrinciple && (
              <div className="text-xs mb-3">
                <span className="opacity-60">Legal Principle: </span>
                <span className="font-medium">{conflict.legalPrinciple}</span>
              </div>
            )}

            {conflict.entities.length > 0 && (
              <div className="flex flex-wrap gap-2 pt-3 border-t border-current/20">
                {conflict.entities.map((entity) => (
                  <span
                    key={entity.id}
                    className="text-xs px-2 py-1 rounded-lg bg-white/10"
                    title={entity.entityType}
                  >
                    {entity.name}: <span className="font-medium">{entity.value}</span>
                  </span>
                ))}
              </div>
            )}
          </div>
        ))}
      </div>
    </div>
  );
}
