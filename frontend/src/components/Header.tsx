"use client";

export default function Header() {
  return (
    <header className="glass border-b border-white/10">
      <div className="container mx-auto px-4 py-4">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-primary-500 to-accent-500 flex items-center justify-center">
              <svg
                className="w-6 h-6 text-white"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z"
                />
              </svg>
            </div>
            <div>
              <h1 className="text-xl font-bold gradient-text">ConflictGuard</h1>
              <p className="text-xs text-gray-400">AI-Powered Document Conflict Detection</p>
            </div>
          </div>

          <div className="flex items-center gap-4">
            <span className="text-xs text-gray-500 hidden sm:block">
              Powered by Claude AI + Neo4j
            </span>
            <div className="w-2 h-2 rounded-full bg-green-500 animate-pulse" title="Backend Connected" />
          </div>
        </div>
      </div>
    </header>
  );
}
