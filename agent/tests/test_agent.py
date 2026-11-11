from main import triage, assess, health
def test_kafka_signal():
    state = {"incident": {"title": "Kafka lag", "description": "consumer timeout"}}
    result = triage(state)
    assert "Kafka messaging symptoms" in result["signals"]
    assert len(result["hypotheses"]) == 2
def test_assessment_is_read_only():
    result = assess({"signals": ["Elevated latency"], "hypotheses": ["Inspect traces"], "runbooks": []})
    assert "No live telemetry" in result["assessment"]
    assert "operator review" in result["recommendations"][-1]
def test_health():
    assert health() == {"status": "ok"}