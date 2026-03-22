package uk.co.asepstrath.bank.evaluation;

import java.util.ArrayList;
import java.util.List;

public class EvaluationOverviewResponse {
    public EvaluationSummary summary;
    public List<EvaluationSectorGroup> bySector = new ArrayList<>();
    public List<EvaluationPositionLine> positions = new ArrayList<>();
}
