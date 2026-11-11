"""Read-only, evidence-first incident investigation workflow."""
import os
import re
from typing import TypedDict
import psycopg
from psycopg.rows import dict_row
from fastapi import FastAPI
from pydantic import BaseModel, Field
from langgraph.graph import StateGraph, END

app = FastAPI(title="Incident Intelligence Agent")

class IncidentRequest(BaseModel):
    incident_id: int
    title: str
    description: str = ""
    service_name: str
    severity: str = "MEDIUM"

class State(TypedDict, total=False):
    incident: dict
    signals: list[str]
    hypotheses: list[str]
    runbooks: list[dict]
    assessment: str
    recommendations: list[str]

RULES = {
    "kafka": ("Kafka messaging symptoms", "Check consumer group lag, partition skew and recent deploys"),
    "timeout": ("Timeout or downstream latency", "Inspect upstream/downstream latency and timeout budgets"),
    "database": ("Database connectivity or query issues", "Review query plans, pool saturation and connection errors"),
    "error": ("Application errors", "Correlate error rates with deployments and trace IDs"),
    "latency": ("Elevated latency", "Compare p95/p99 latency against baseline and dependency timings"),
}

def triage(state: State) -> dict:
    text = (state["incident"]["title"] + " " + state["incident"]["description"]).lower()
    matched = [(signal, advice) for key, (signal, advice) in RULES.items() if key in text]
    if not matched:
        matched = [("Unclassified incident", "Inspect service logs, traces, metrics and recent changes")]
    return {"signals": [s for s, _ in matched], "hypotheses": [a for _, a in matched]}

def retrieve(state: State) -> dict:
    incident = state["incident"]
    query = " ".join([incident["title"], incident["description"], incident["service_name"]])
    db_url = os.environ.get("DATABASE_URL")
    if not db_url:
        return {"runbooks": []}
    with psycopg.connect(db_url, row_factory=dict_row) as conn:
        with conn.cursor() as cur:
            cur.execute("""
                SELECT title, content FROM runbooks
                WHERE search_vector @@ plainto_tsquery('english', %s)
                ORDER BY ts_rank(search_vector, plainto_tsquery('english', %s)) DESC
                LIMIT 3
            """, (query, query))
            return {"runbooks": list(cur.fetchall())}

def assess(state: State) -> dict:
    evidence = state.get("runbooks", [])
    notes = "; ".join(state["signals"])
    assessment = f"Preliminary text-based triage: {notes}. No live telemetry has been checked."
    if evidence:
        assessment += f" Retrieved {len(evidence)} potentially relevant runbook(s); validate against live evidence."
    return {"assessment": assessment, "recommendations": state["hypotheses"] + ["Require operator review before remediation"]}

workflow = StateGraph(State)
workflow.add_node("triage", triage)
workflow.add_node("retrieve", retrieve)
workflow.add_node("assess", assess)
workflow.set_entry_point("triage")
workflow.add_edge("triage", "retrieve")
workflow.add_edge("retrieve", "assess")
workflow.add_edge("assess", END)
graph = workflow.compile()

@app.get("/health")
def health():
    return {"status": "ok"}

@app.post("/investigate")
def investigate(incident: IncidentRequest):
    result = graph.invoke({"incident": incident.model_dump()})
    return {key: result[key] for key in ("signals", "hypotheses", "runbooks", "assessment", "recommendations")}
