"use client";

import { useCallback, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import {
  AlertTriangle,
  RefreshCw,
  Trash2,
  ChevronDown,
  ChevronUp,
} from "lucide-react";
import { api } from "@/lib/api";
import { useAuth } from "@/context/AuthContext";
import { StatusBadge } from "@/components/OrderStatusBadge";
import { Pagination } from "@/components/Pagination";
import type { Page, FailedEvent } from "@/lib/types";

type ServiceName = "order" | "inventory" | "payment";

interface ServiceTab {
  name: ServiceName;
  label: string;
  count: number;
}

export default function FailedEventsPage() {
  const router = useRouter();
  const { user, isAdmin, loading: authLoading } = useAuth();
  const [activeService, setActiveService] = useState<ServiceName>("order");
  const [events, setEvents] = useState<FailedEvent[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);
  const [expandedId, setExpandedId] = useState<number | null>(null);
  const [actionLoading, setActionLoading] = useState<number | null>(null);
  const [tabs, setTabs] = useState<ServiceTab[]>([
    { name: "order", label: "Order Service", count: 0 },
    { name: "inventory", label: "Inventory Service", count: 0 },
    { name: "payment", label: "Payment Service", count: 0 },
  ]);

  useEffect(() => {
    if (authLoading) return;
    if (!user || !isAdmin) {
      router.push("/");
      return;
    }
    fetchCounts();
  }, [user, isAdmin, authLoading, router]);

  const fetchCounts = useCallback(async () => {
    const services: ServiceName[] = ["order", "inventory", "payment"];
    const counts = await Promise.all(
      services.map((s) =>
        api
          .get<{ failedCount: number }>(
            `/api/admin/failed-events/count?service=${s}`,
          )
          .then((r) => r.failedCount)
          .catch(() => 0),
      ),
    );
    setTabs((prev) =>
      prev.map((tab, i) => ({ ...tab, count: counts[i] })),
    );
  }, []);

  useEffect(() => {
    if (authLoading || !user || !isAdmin) return;
    fetchEvents();
  }, [activeService, page, user, isAdmin, authLoading]);

  const fetchEvents = useCallback(async () => {
    setLoading(true);
    try {
      const data = await api.get<Page<FailedEvent>>(
        `/api/admin/failed-events?service=${activeService}&page=${page}&size=10`,
      );
      setEvents(data.content);
      setTotalPages(data.totalPages);
    } catch {
      setEvents([]);
    } finally {
      setLoading(false);
    }
  }, [activeService, page]);

  const handleRetry = async (id: number) => {
    setActionLoading(id);
    try {
      await api.post(
        `/api/admin/failed-events/${id}/retry?service=${activeService}`,
      );
      await fetchEvents();
      await fetchCounts();
    } catch {
      // silently handle
    } finally {
      setActionLoading(null);
    }
  };

  const handleDiscard = async (id: number) => {
    setActionLoading(id);
    try {
      await api.post(
        `/api/admin/failed-events/${id}/discard?service=${activeService}`,
      );
      await fetchEvents();
      await fetchCounts();
    } catch {
      // silently handle
    } finally {
      setActionLoading(null);
    }
  };

  if (authLoading || !user || !isAdmin) return null;

  return (
    <div className="max-w-6xl mx-auto px-4 py-8">
      <div className="flex items-center gap-3 mb-6">
        <AlertTriangle size={28} className="text-[var(--color-danger)]" />
        <h1 className="text-2xl font-bold">Failed Events Dashboard</h1>
      </div>

      {/* Service tabs */}
      <div className="flex gap-2 mb-6">
        {tabs.map((tab) => (
          <button
            key={tab.name}
            onClick={() => {
              setActiveService(tab.name);
              setPage(0);
            }}
            className={`px-4 py-2 rounded-md text-sm font-medium transition-colors ${
              activeService === tab.name
                ? "bg-[var(--color-primary)] text-white"
                : "bg-white text-[var(--color-text)] hover:bg-gray-100 border border-gray-200"
            }`}
          >
            {tab.label}
            {tab.count > 0 && (
              <span className="ml-2 bg-red-500 text-white text-xs rounded-full px-2 py-0.5">
                {tab.count}
              </span>
            )}
          </button>
        ))}
      </div>

      {loading ? (
        <div className="space-y-3">
          {Array.from({ length: 5 }).map((_, i) => (
            <div key={i} className="bg-white rounded-lg shadow-sm h-16 animate-pulse" />
          ))}
        </div>
      ) : events.length === 0 ? (
        <div className="bg-white rounded-lg shadow-sm p-12 text-center">
          <p className="text-[var(--color-success)] text-lg font-medium">
            No unresolved events
          </p>
          <p className="text-sm text-[var(--color-text-muted)] mt-1">
            All events for {activeService} service are resolved.
          </p>
        </div>
      ) : (
        <>
          <div className="space-y-3">
            {events.map((event) => (
              <div
                key={event.id}
                className="bg-white rounded-lg shadow-sm overflow-hidden"
              >
                <div
                  className="flex items-center gap-4 px-4 py-3 cursor-pointer hover:bg-gray-50 transition-colors"
                  onClick={() =>
                    setExpandedId(expandedId === event.id ? null : event.id)
                  }
                >
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2 mb-1">
                      <span className="text-sm font-medium">#{event.id}</span>
                      <StatusBadge status={event.status} type="failedEvent" />
                      <span className="text-xs bg-gray-100 text-gray-600 px-2 py-0.5 rounded">
                        {event.topic}
                      </span>
                    </div>
                    <p className="text-xs text-[var(--color-text-muted)] truncate">
                      Key: {event.eventKey} | Retries: {event.retryCount}/
                      {event.maxRetries} |{" "}
                      {new Date(event.createdAt).toLocaleString()}
                    </p>
                  </div>

                  <div className="flex items-center gap-2 shrink-0">
                    {event.status === "FAILED" && (
                      <>
                        <button
                          onClick={(e) => {
                            e.stopPropagation();
                            handleRetry(event.id);
                          }}
                          disabled={actionLoading === event.id}
                          className="flex items-center gap-1 text-xs bg-blue-500 hover:bg-blue-600 text-white px-3 py-1.5 rounded-md transition-colors disabled:opacity-50"
                        >
                          <RefreshCw
                            size={12}
                            className={
                              actionLoading === event.id ? "animate-spin" : ""
                            }
                          />
                          Retry
                        </button>
                        <button
                          onClick={(e) => {
                            e.stopPropagation();
                            handleDiscard(event.id);
                          }}
                          disabled={actionLoading === event.id}
                          className="flex items-center gap-1 text-xs bg-gray-500 hover:bg-gray-600 text-white px-3 py-1.5 rounded-md transition-colors disabled:opacity-50"
                        >
                          <Trash2 size={12} />
                          Discard
                        </button>
                      </>
                    )}
                    {expandedId === event.id ? (
                      <ChevronUp size={16} />
                    ) : (
                      <ChevronDown size={16} />
                    )}
                  </div>
                </div>

                {expandedId === event.id && (
                  <div className="border-t border-gray-200 px-4 py-3 bg-gray-50">
                    <div className="grid md:grid-cols-2 gap-4 text-sm">
                      <div>
                        <p className="font-medium text-xs text-[var(--color-text-muted)] mb-1">
                          Error Message
                        </p>
                        <p className="text-[var(--color-danger)]">
                          {event.errorMessage}
                        </p>
                      </div>
                      <div>
                        <p className="font-medium text-xs text-[var(--color-text-muted)] mb-1">
                          Event Payload
                        </p>
                        <pre className="bg-white border border-gray-200 rounded-md p-2 text-xs overflow-x-auto max-h-40">
                          {formatJson(event.payload)}
                        </pre>
                      </div>
                    </div>
                  </div>
                )}
              </div>
            ))}
          </div>
          <Pagination
            currentPage={page}
            totalPages={totalPages}
            onPageChange={setPage}
          />
        </>
      )}
    </div>
  );
}

function formatJson(str: string): string {
  try {
    return JSON.stringify(JSON.parse(str), null, 2);
  } catch {
    return str;
  }
}
