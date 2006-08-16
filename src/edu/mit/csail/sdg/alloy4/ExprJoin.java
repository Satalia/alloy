package edu.mit.csail.sdg.alloy4;

/**
 * Immutable; represents a relational join expression.
 * @author Felix Chang
 */

public final class ExprJoin extends Expr {

    /**
     * Accepts the return visitor.
     * @see edu.mit.csail.sdg.alloy4.VisitReturn
     */
    @Override public Object accept(VisitReturn visitor) {
        return visitor.accept(this);
    }

    /**
     * Accepts the desugar visitor.
     * @see edu.mit.csail.sdg.alloy4.VisitDesugar
     */
    @Override public Expr accept(VisitDesugar visitor) {
        return visitor.accept(this);
    }

    /**
     * Accepts the desugar2 visitor.
     * @see edu.mit.csail.sdg.alloy4.VisitDesugar2
     */
    @Override public Expr accept(VisitDesugar2 visitor, Type type) {
        return visitor.accept(this,type);
    }

    /**
     * The left-hand-side expression.
     */
    public final Expr left;

    /**
     * The right-hand-side expression.
     */
    public final Expr right;

    /**
     * Constructs an untypechecked ExprJoin expression.
     * @param pos - the original position in the file
     * @param left - the left-hand-side expression
     * @param right - the right-hand-side expression
     */
    public ExprJoin (Pos pos, Expr left, Expr right) {
        this(pos, left, right, null);
    }

    /**
     * Constructs a typechecked ExprJoin expression.
     * @param pos - the original position in the file
     * @param left - the left-hand-side expression
     * @param right - the right-hand-side expression
     * @param type - the type
     */
    public ExprJoin (Pos pos, Expr left, Expr right, Type type) {
        super(pos, type, 0);
        if (left.mult>0)
            throw left.syntaxError("Multiplicity expression not allowed here");
        if (right.mult>0)
            throw right.syntaxError("Multiplicity expression not allowed here");
        this.left=nonnull(left);
        this.right=nonnull(right);
    }
}
