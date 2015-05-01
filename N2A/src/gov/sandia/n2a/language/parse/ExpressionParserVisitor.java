/* Generated By:JavaCC: Do not edit this line. ExpressionParserVisitor.java Version 6.0_1 */
package gov.sandia.n2a.language.parse;

public interface ExpressionParserVisitor
{
  public Object visit(SimpleNode node, Object data) throws ParseException;
  public Object visit(ASTStart node, Object data) throws ParseException;
  public Object visit(ASTOpNode node, Object data) throws ParseException;
  public Object visit(ASTListNode node, Object data) throws ParseException;
  public Object visit(ASTUnitNode node, Object data) throws ParseException;
  public Object visit(ASTMatrixNode node, Object data) throws ParseException;
  public Object visit(ASTVarNode node, Object data) throws ParseException;
  public Object visit(ASTFunNode node, Object data) throws ParseException;
  public Object visit(ASTConstant node, Object data) throws ParseException;
}
/* JavaCC - OriginalChecksum=f0766b9d1da54997444032fccb040a71 (do not edit this line) */