package dev.incident;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import jakarta.validation.Valid;
@RestController
@RequestMapping("/api/incidents")
public class IncidentController {
  private final IncidentRepository incidents;
  private final InvestigationRepository investigations;
  private final RestClient client;
  public IncidentController(IncidentRepository incidents, InvestigationRepository investigations, @Value("${agent.url}") String agentUrl) {
    this.incidents=incidents; this.investigations=investigations; this.client=RestClient.builder().baseUrl(agentUrl).build();
  }
  @GetMapping public List<Incident> list() { return incidents.findAll(); }
  @PostMapping @ResponseStatus(HttpStatus.CREATED) public Incident create(@Valid @RequestBody Incident input) {
    input.id=null; input.createdAt=java.time.Instant.now(); input.status="OPEN";
    if (input.severity == null || !Set.of("LOW","MEDIUM","HIGH","CRITICAL").contains(input.severity)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid severity");
    return incidents.save(input);
  }
  @GetMapping("/{id}") public Incident get(@PathVariable Long id) { return require(id); }
  @PatchMapping("/{id}/status") public Incident status(@PathVariable Long id,@RequestBody Map<String,String> body) {
    Incident incident=require(id); String status=body.get("status");
    if (status == null || !Set.of("OPEN","INVESTIGATING","MITIGATED","RESOLVED").contains(status)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid status");
    incident.status=status; return incidents.save(incident);
  }
  @GetMapping("/{id}/investigations") public List<Investigation> history(@PathVariable Long id) {
    require(id); return investigations.findByIncidentIdOrderByCreatedAtDesc(id);
  }
  @PostMapping(value="/{id}/investigate", produces=MediaType.APPLICATION_JSON_VALUE) public String investigate(@PathVariable Long id) {
    Incident incident=require(id); incident.status="INVESTIGATING"; incidents.save(incident);
    try {
      String result=client.post().uri("/investigate").contentType(MediaType.APPLICATION_JSON)
        .body(Map.of("incident_id",incident.id,"title",incident.title,"description",incident.description==null?"":incident.description,"service_name",incident.serviceName,"severity",incident.severity))
        .retrieve().body(String.class);
      investigations.save(new Investigation(id,result)); return result;
    } catch (Exception ex) {
      throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"Investigation service unavailable",ex);
    }
  }
  private Incident require(Long id) { return incidents.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Incident not found")); }
}
