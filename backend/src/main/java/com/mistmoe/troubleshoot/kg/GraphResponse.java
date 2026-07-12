package com.mistmoe.troubleshoot.kg;

import java.util.List;

public record GraphResponse(List<GraphNode> nodes, List<GraphLink> links) {
}
