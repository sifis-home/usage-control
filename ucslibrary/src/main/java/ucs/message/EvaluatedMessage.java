package ucs.message;

import ucs.pdp.PDPEvaluation;

/**
 * A message that has an evaluation
 *
 * @author Alessandro Rosetti
 */
public interface EvaluatedMessage {

    public PDPEvaluation getEvaluation();

    public void setEvaluation( PDPEvaluation evaluation );

}
