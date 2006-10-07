package edu.mit.csail.sdg.alloy4.node;

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.Set;
import edu.mit.csail.sdg.alloy4.helper.ErrorInternal;
import edu.mit.csail.sdg.alloy4.helper.ErrorSyntax;
import edu.mit.csail.sdg.alloy4.helper.Pos;

/**
 * Mutable; reresents a "signature".
 * @author Felix Chang
 */

public final class ParaSig extends Para {

    public static final ParaSig UNIV=new ParaSig("univ",true,null);
    public static final ParaSig NONE=new ParaSig("none",false,null);
    public static final ParaSig SIGINT=new ParaSig("Int",false,UNIV);

    // Abstract or not. Note, if a sig is abstract, it cannot be a subset sig (ie. "in" field must be null)
    public final boolean abs;

    // At most 1 can be true
    public final boolean lone,one,some;

    public List<String> aliases=new ArrayList<String>();

    // The list of field declarations (in 2 data structures)
    public List<VarDecl> decls;
    // The list of field objects. fields.size() must equal FieldDecl.count(decls).
    public List<Field> fields;
    // If non-null, it is an "appended facts" paragraph
    public Expr appendedFacts;

    public final String fullname;

    // The following 4 fields are initially empty until we properly initialize them
    // (Though "type" will be set already, for ParaSig.UNIV/NONE/SIGINT)
    public Type type;

    public boolean toplevel() { return !subset && (sup()==null || sup()==ParaSig.UNIV); }

    private Object sup;                        // If I'm a SUBSIG, this is the parent. ELSE null.

    public ParaSig sup() {
        if (sup==null) return null;
        if (sup instanceof ParaSig) return ((ParaSig)sup);
        throw new ErrorInternal(pos, "Sig \""+fullname+"\" should have resolved its sup field!");
    }

    public void resolveSup(Unit u) {
        if (!(sup instanceof String)) return;
        Set<Object> ans=u.lookup_sigORparam((String)sup);
        if (ans.size()>1) throw new ErrorSyntax(pos, "Sig \""+fullname+"\" tries to extend \""+((String)sup)+"\", but that name is ambiguous.");
        if (ans.size()<1) throw new ErrorSyntax(pos, "Sig \""+fullname+"\" tries to extend a non-existent signature \""+((String)sup)+"\"");
        ParaSig parent=(ParaSig)(ans.iterator().next());
        if (parent==ParaSig.NONE)   throw new ErrorSyntax(pos, "Sig \""+fullname+"\" cannot extend the builtin \"none\" signature");
        if (parent==ParaSig.SIGINT) throw new ErrorSyntax(pos, "Sig \""+fullname+"\" cannot extend the builtin \"Int\" signature");
        if (parent.subset) throw new ErrorSyntax(pos, "Sig \""+fullname+"\" cannot extend a subset signature \""+parent.fullname+"\"! A signature can only extend a toplevel signature or a subsignature.");
        sup=parent;
    }

    private List<Object> sups=new ArrayList<Object>();

    public Iterable<ParaSig> sups() {
        if (sups.size()>0 && !(sups.get(0) instanceof ParaSig))
            throw new ErrorInternal(pos, "Sig \""+fullname+"\" should have resolved its sups field!");
        return new Iterable<ParaSig>() {
            public final Iterator<ParaSig> iterator() {
                return new Iterator<ParaSig>() {
                    private int i=0;
                    public final boolean hasNext() { return i<sups.size(); }
                    public final ParaSig next() { if (i>=sups.size()) throw new NoSuchElementException(); return (ParaSig)sups.get(i++); }
                    public final void remove() { throw new UnsupportedOperationException(); }
                };
            }
        };
    }

    public void resolveSups(Unit u) {
        if (sups.size()==0 || (sups.get(0) instanceof ParaSig)) return;
        for(int i=0; i<sups.size(); i++) {
            String n=(String)(sups.get(i));
            Set<Object> ans=u.lookup_sigORparam(n);
            if (ans.size()>1) throw new ErrorSyntax(pos, "Sig \""+fullname+"\" tries to be a subset of \""+n+"\", but the name \""+n+"\" is ambiguous.");
            if (ans.size()<1) throw new ErrorSyntax(pos, "Sig \""+fullname+"\" tries to be a subset of a non-existent signature \""+n+"\"");
            ParaSig parent=(ParaSig)(ans.iterator().next());
            if (parent==ParaSig.NONE) throw new ErrorSyntax(pos, "Sig \""+fullname+"\" cannot be a subset of the builtin \"none\" signature");
            if (parent==ParaSig.UNIV) throw new ErrorSyntax(pos, "Sig \""+fullname+"\" is already implicitly a subset of the builtin \"univ\" signature");
            sups.set(i,parent);
        }
    }

    public List<ParaSig> subs=new ArrayList<ParaSig>(); // If I'm a TOPSIG/SUBSIG/"univ", sigs who EXTEND me.
    public final boolean subset;

    public ParaSig(Pos p, String al, String n, boolean fa, boolean fl, boolean fo, boolean fs,
            List<String> i, String e, List<VarDecl> d, Expr f) {
        super(p, al, n);
        if (al.length()==0) fullname="/"+n; else fullname="/"+al+"/"+n;
        aliases.add(al);
        abs=fa; lone=fl; one=fo; some=fs;
        if (n==null || d==null) throw this.internalError("NullPointerException in Sig constructor!");
        if (n.length()==0) throw this.syntaxError("A signature must have a name!");
        if (n.indexOf('/')>=0) throw this.syntaxError("Signature name must not contain \'/\'.");
        if ((lone && one) || (lone && some) || (one && some)) throw this.syntaxError("A signature definition can only include at most one of the three keywords: ONE, LONE, and SOME.");

        if (i==null) subset=false; else {
            subset=true;
            if (abs) throw this.syntaxError("A subset signature cannot be abstract!");
            if (e!=null) throw this.syntaxError("A signature cannot both be a subset signature and a subsignature!");
            if (i.size()==0) throw this.syntaxError("To declare a subset signature, you must give the names of its parent signatures!");
            for(String ii:i) sups.add(nonnull(ii));
        }

        if (!subset && e==null) sup="univ"; else sup=e;

        fields=new ArrayList<Field>();
        decls=new ArrayList<VarDecl>(d);
        for(VarDecl dd:decls) {
            List<String> names=new ArrayList<String>();
            for(String dn:dd.names) {
                Field x=new Field(dd.value.pos, dn, (path.length()==0?"/"+name:"/"+path+"/"+name)+"."+dn);
                fields.add(x);
                names.add(dn);
            }
        }
        String dup=VarDecl.hasDuplicateName(decls);
        if (dup!=null) throw this.syntaxError("This signature cannot have two fields with the same name: \""+dup+"\"");

        appendedFacts=f;

        type=null;
    }

    public boolean isSubtypeOf(ParaSig other) {
        if (subset || other.subset) return false; // This method is undefined for SUBSETSIG
        if (this==NONE || this==other || other==UNIV) return true;
        if (other==NONE) return false;
        for(ParaSig me=this; me!=null; me=me.sup()) if (me==other) return true;
        return false;
    }

    public ParaSig intersect(ParaSig other) {
        if (subset || other.subset) return NONE; // This method is undefined for SUBSETSIG
        if (this.isSubtypeOf(other)) return this;
        if (other.isSubtypeOf(this)) return other;
        return NONE;
    }

    private ParaSig(String n, boolean isAbstract, ParaSig myParent) {
        super(new Pos("$builtin$",1,1), "", n);
        fullname="/"+n;
        aliases.add("");
        abs=isAbstract; lone=false; one=false; some=false;
        decls=new ArrayList<VarDecl>(0);
        appendedFacts=null;
        type=Type.make(this);
        fields=new ArrayList<Field>(0);
        sup=myParent; if (myParent!=null) myParent.subs.add(this);
        subset=false;
    }

    public boolean isEmpty() { return this==NONE; }
    public boolean isNonEmpty() { return this!=NONE; }
}