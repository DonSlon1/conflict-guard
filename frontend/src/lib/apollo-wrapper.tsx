"use client";

import { ApolloClient, InMemoryCache, ApolloProvider } from "@apollo/client";

// Use localhost for browser access (not Docker internal hostname)
const graphqlUrl = typeof window !== 'undefined'
  ? "http://localhost:8080/graphql"  // Browser
  : (process.env.NEXT_PUBLIC_GRAPHQL_URL || "http://backend:8080/graphql"); // Server

const client = new ApolloClient({
  uri: graphqlUrl,
  cache: new InMemoryCache(),
});

export function ApolloWrapper({ children }: { children: React.ReactNode }) {
  return <ApolloProvider client={client}>{children}</ApolloProvider>;
}
