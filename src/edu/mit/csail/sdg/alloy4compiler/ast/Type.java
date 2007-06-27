/*
 * Alloy Analyzer
 * Copyright (c) 2007 Massachusetts Institute of Technology
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA,
 * 02110-1301, USA
 */

package edu.mit.csail.sdg.alloy4compiler.ast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import edu.mit.csail.sdg.alloy4.Err;
import edu.mit.csail.sdg.alloy4.ErrorFatal;
import edu.mit.csail.sdg.alloy4.ErrorType;
import edu.mit.csail.sdg.alloy4.ConstList;
import edu.mit.csail.sdg.alloy4.ConstList.TempList;
import edu.mit.csail.sdg.alloy4compiler.ast.Sig.PrimSig;
import static edu.mit.csail.sdg.alloy4compiler.ast.Sig.UNIV;

/**
 * Immutable; represents the type of an expression.
 *
 * <p> Note: except for "toString()" and "fold()", the return value of every method is always valid for all time;
 * for example, given types A and B, and you call C=A.intersect(B), then the result C will always be
 * the intersection of A and B even if the caller later constructs more sigs or subsigs or subsetsigs...
 */

public final class Type implements Iterable<Type.ProductType> {

    /** The maximum allowed arity; due to the way we represent arities, this must be between 2 and 30. */
    private static final int MAXARITY = 30;

    /**
     * Immutable; represents a list of PrimSig objects.
     *
     * <p> <b>Invariant:</b>  0 < types.length <= MAXARITY
     * <p> <b>Invariant:</b>  "one of the sig in the list is NONE" iff "every sig in the list is NONE".
     *
     * <p> Note: the return value of every method is always valid for all time;
     * for example, given ProductType A and B, and you call C=A.intersect(B), then the result C will always be
     * the intersection of A and B even if the caller later constructs more sigs or subsigs or subsetsigs...
     */
    public static final class ProductType {

        /** The array of PrimSig objects. */
        private final PrimSig[] types;

        /**
         * Constructs a new ProductType object consisting of the given array of PrimSig objects.
         *
         * <p> Precondition:  0 < types.length <= MAXARITY
         * <p> Precondition:  "one of the sig in the list is NONE" iff "every sig in the list is NONE"
         *
         * <p> Note: it will use the array as-is, so the caller should give up the reference to the array.
         */
        private ProductType(PrimSig[] array) {
            types = array;
        }

        /** Constructs a new ProductType made of exactly 1 PrimSig; it promises it will not call any method or read anything from "sig". */
        private ProductType(PrimSig sig) {
            types = new PrimSig[]{sig};
        }

        /**
         * Constructs a new ProductType made of exactly N references to the same PrimSig object.
         * <p> Precondition: n > 0
         */
        private ProductType(int n, PrimSig sig) {
            types = new PrimSig[n];
            for(int i=0; i<n; i++) { types[i] = sig; }
        }

        /** Returns a hash code consistent with equals() */
        @Override public int hashCode() { return types[0].hashCode(); }

        /** Returns true if this.arity==that.arity and this.types[i]==that.types[i] for each i */
        @Override public boolean equals(Object that) {
            if (this==that) return true;
            if (!(that instanceof ProductType)) return false;
            ProductType x=(ProductType)that;
            if (types.length != x.types.length) return false;
            for(int i=types.length-1; i>=0; i--) if (types[i]!=x.types[i]) return false;
            return true;
        }

        /**
         * Returns true if (this[i] is equal or subtype of that[i]) for every i.
         * <p> Precondition: this.arity == that.arity
         */
        private boolean isSubtypeOf(ProductType that) {
            for(int i=types.length-1; i>=0; i--) if (!types[i].isSubtypeOf(that.types[i])) return false;
            return true;
        }

        /** Returns the arity of this ProductType object. */
        public int arity() { return types.length; }

        /**
         * Returns a specific PrimSig in this ProductType
         * @throws ArrayIndexOutOfBoundsException if (i < 0) or (i >= arity)
         */
        PrimSig get(int i) { return types[i]; }

        /** Returns true if this == NONE->..->NONE */
        public boolean isEmpty() { return types[0]==Sig.NONE; }

        /**
         * Returns the tranpose of this
         * <p> Precondition: this.arity()==2
         */
        private ProductType transpose() {
            return new ProductType(new PrimSig[]{types[1], types[0]});
        }

        /**
         * Returns the cross product of this and that.
         *
         * <p> Note: If either or both is NONE->..->NONE, then we return NONE->..->NONE instead.
         *
         * @throws ErrorType if this.arity + that.arity > MAXARITY
         */
        ProductType product(ProductType that) throws ErrorType {
            final int n = types.length + that.types.length;
            if (n > MAXARITY) throw new ErrorType("Relation of arity > "+MAXARITY+" is unsupported.");
            if (isEmpty() || that.isEmpty()) return new ProductType(n, Sig.NONE);
            final PrimSig[] ans = new PrimSig[n];
            int j=0;
            for(int i=0; i<this.types.length; i++, j++) { ans[j]=this.types[i]; }
            for(int i=0; i<that.types.length; i++, j++) { ans[j]=that.types[i]; }
            return new ProductType(ans);
        }

        /**
         * Returns the intersection of this and that.
         *
         * <p> Note: if (this[i] & that[i]) is empty for at least one i, then we return "NONE->..->NONE" instead.
         *
         * <p> Precondition: this.arity == that.arity
         */
        private ProductType intersect(ProductType that) {
            final int n = types.length;
            final PrimSig[] ans = new PrimSig[n];
            for(int i=0; i<n; i++) {
                PrimSig c = this.types[i].intersect(that.types[i]);
                if (c==Sig.NONE) { for(i=0; i<n; i++) ans[i]=c; break; }
                ans[i]=c;
            }
            return new ProductType(ans);
        }

        /**
         * Returns true iff the intersection of this and that is nonempty.
         *
         * <p> Precondition: this.arity == that.arity
         */
        private boolean intersects(ProductType that) {
            for(int i=types.length-1; i>=0; i--) if (!types[i].intersects(that.types[i])) return false;
            return true;
        }

        /**
         * Returns the relational join of this and that.
         *
         * <p> Note: If (this.rightmost & that.leftmost) is empty, we return NONE->..->NONE instead.
         *
         * @throws ErrorType if this.arity()==1 and that.arity()==1, or this.arity()+that.arity()-2 > MAXARITY
         */
        ProductType join(ProductType that) throws ErrorType {
            int left=types.length, right=that.types.length;
            if (left<=1 && right<=1) throw new ErrorType("You cannot perform relational join between two unary sets.");
            final int n=left+right-2;
            if (n > MAXARITY) throw new ErrorType("Relation of arity > "+MAXARITY+" is unsupported.");
            final PrimSig a=types[left-1], b=that.types[0], c=a.intersect(b);
            if (c==Sig.NONE) return new ProductType(n, c);
            final PrimSig[] types = new PrimSig[n];
            int j=0;
            for(int i=0; i<left-1; i++, j++) { types[j]=this.types[i]; }
            for(int i=1; i<right; i++, j++)  { types[j]=that.types[i]; }
            return new ProductType(types);
        }

        /**
         * If (this[i] & that) is not empty, then return this[0]->this[1]->this[2]->this[3]..->this[n-1]
         * except the i-th entry is replaced by (this[i] & that).
         *
         * <p> Otherwise, this method returns NONE->..->NONE
         */
        ProductType columnRestrict(PrimSig that, int i) {
            if (i<0 || i>=types.length) return this;
            that = types[i].intersect(that);
            if (that==types[i]) return this;
            if (that==Sig.NONE) return new ProductType(types.length, that);
            final PrimSig[] newlist = new PrimSig[types.length];
            for(int j=0; j<types.length; j++) { newlist[j]=types[j]; }
            newlist[i] = that;
            return new ProductType(newlist);
        }

        /** Returns the String representation of this ProductType object. */
        @Override public String toString() {
            StringBuilder ans=new StringBuilder();
            for(int i=0; i<types.length; i++) { if (i!=0) ans.append("->"); ans.append(types[i]); }
            return ans.toString();
        }
    }

    /** Constant value with is_int==false, is_bool==false, and entries.size()==0. */
    public static final Type EMPTY = new Type(false, false, null, 0);

    /** Constant value with is_int==true, is_bool==false, and entries.size()==0. */
    public static final Type INT = new Type(true, false, null, 0);

    /** Constant value with is_int==false, is_bool==true, and entries.size()==0. */
    public static final Type FORMULA = new Type(false, true, null, 0);

    /** Constant value with is_int==true, is_bool==true, and entries.size()==0. */
    public static final Type INTANDFORMULA = new Type(true, true, null, 0);

    /** True if primitive integer value is a possible value in this type. */
    public final boolean is_int;

    /** True if primitive boolean value is a possible value in this type. */
    public final boolean is_bool;

    /**
     * Contains the set of arities in this type.
     * <br> Each possible arity corresponds to one of the bit.
     * <br> The (1<<1) bitmask is nonzero iff arity 1 exists
     * <br> The (1<<2) bitmask is nonzero iff arity 2 exists
     * <br> The (1<<3) bitmask is nonzero iff arity 3 exists
     * <br> ...
     */
    private final int arities;

    /** Contains the list of ProductType entries in this type. */
    private final ConstList<ProductType> entries;

    /**
     * Returns an iterator that iterates over the ProductType entries in this type.
     * <p> This iterator will reject all modification requests.
     */
    public Iterator<ProductType> iterator() { return entries.iterator(); }

    /**
     * Merge "x" into the set of entries, then return the new arity bitmask.
     * <br> Precondition: 0 < entries.size() <= MAXARITY
     * <br> Precondition: entries and arities are consistent
     */
    private static int add(TempList<ProductType> entries, int arities, ProductType x) {
        if (x==null) return arities;
        final int arity=x.types.length;
        // If x is subsumed by a ProductType in this, return. Likewise, remove all entries in this that are subsumed by x.
        for(int n=entries.size(), i=n-1; i>=0; i--) {
            ProductType y=entries.get(i);
            if (y.types.length != arity) continue;
            if (x.isSubtypeOf(y)) return arities;
            if (y.isSubtypeOf(x)) {n--; entries.set(i, entries.get(n)); entries.remove(n);}
        }
        arities = arities | (1 << arity);
        entries.add(x);
        return arities;
    }

    /**
     * Create a new type consisting of the given set of entries, set of arities, and the given is_int/is_bool values;
     * <p> Precondition: entries and arities must be consistent
     */
    private Type(boolean is_int, boolean is_bool, ConstList<ProductType> entries, int arities) {
        this.is_int = is_int;
        this.is_bool = is_bool;
        if (entries==null || entries.size()==0 || arities==0) {
            this.entries = ConstList.make();
            this.arities = 0;
        } else {
            this.entries = entries;
            this.arities = arities;
        }
    }

    /**
     * Create a new type consisting of the given set of entries, set of arities, and the given is_int/is_bool values;
     * <p> Precondition: entries and arities must be consistent
     */
    private static Type make(boolean is_int, boolean is_bool, ConstList<ProductType> entries, int arities) {
        if (entries==null || entries.size()==0 || arities==0) {
            if (is_int) return is_bool?INTANDFORMULA:INT; else return is_bool?FORMULA:EMPTY;
        }
        return new Type(is_int, is_bool, entries, arities);
    }

    /**
     * Create the type consisting of the given ProductType entry.
     */
    static Type make(ProductType productType) {
        return make(false, false, ConstList.make(1,productType), 1 << productType.arity());
    }

    /**
     * Create the type list[start]->list[start+1]->..->list[end-1]
     *
     * @throws ErrorFatal if start<0, end<0, start>=end
     * @throws ErrorType if the resulting relation arity is > MAXARITY
     */
    static Type make(List<PrimSig> list, int start, int end) throws ErrorType, ErrorFatal {
        if (start<0 || end<0 || end>list.size() || start>=end) throw new ErrorFatal("Illegal arity.");
        if (end-start > MAXARITY) throw new ErrorType("Relation of arity > "+MAXARITY+" is unsupported.");
        PrimSig[] newlist = new PrimSig[end-start];
        int j=0;
        for(int i=start; i<end; i++) {
            PrimSig x=list.get(i);
            if (x==Sig.NONE) {
                for(j=0; j<newlist.length; j++) newlist[j]=x;
                break;
            }
            newlist[j]=x;
            j++;
        }
        return make(new ProductType(newlist));
    }

    /**
     * Create the type "sig"; it promises it will not call any method or read anything from "sig".
     */
    static Type make(PrimSig sig) {
        return make(new ProductType(sig));
    }

    /**
     * Create the type "sig->sig".
     */
    static Type make2(PrimSig sig) {
        return make(new ProductType(2,sig));
    }

    /**
     * Create a new type that is the same as "old", except the "is_int" flag is set to true.
     */
    static Type makeInt(Type old) {
        if (old.is_int) return old; else return make(true, old.is_bool, old.entries, old.arities);
    }

    /**
     * Create a new type that is the same as "old", except the "is_bool" flag is set to true.
     */
    static Type makeBool(Type old) {
        if (old.is_bool) return old; else return make(old.is_int, true, old.entries, old.arities);
    }

    /**
     * Create a new type that is the same as "old", except the "is_bool" and "is_int" flags are both set to false.
     */
    static Type removesBoolAndInt(Type old) {
        if (!old.is_bool && !old.is_int) return old; else return make(false, false, old.entries, old.arities);
    }

    /**
     * Returns true iff ((this subsumes that) and (that subsumes this))
     */
    @Override public boolean equals(Object that) {
        if (this==that) return true;
        if (!(that instanceof Type)) return false;
        Type x = (Type)that;
        if (arities != x.arities || is_int != x.is_int || is_bool != x.is_bool) return false;
        again1:
        for(ProductType aa:entries) {
            for(ProductType bb:x.entries) if (aa.types.length==bb.types.length && aa.isSubtypeOf(bb)) continue again1;
            return false;
        }
        again2:
        for(ProductType bb:x.entries) {
            for(ProductType aa:entries) if (aa.types.length==bb.types.length && bb.isSubtypeOf(aa)) continue again2;
            return false;
        }
        return true;
    }

    /** Returns a hash code consistent with equals() */
    @Override public int hashCode() { return arities * (is_int?1732051:1) * (is_bool?314157:1); }

    /** Returns true if this.size()==0 or every entry consists only of "none". */
    public boolean hasNoTuple() {
        for(int i=entries.size()-1; i>=0; i--) if (!entries.get(i).isEmpty()) return false;
        return true;
    }

    /** Returns true if this.size()>0 and at least one entry consists of something other than "none". */
    public boolean hasTuple() {
        for(int i=entries.size()-1; i>=0; i--) if (!entries.get(i).isEmpty()) return true;
        return false;
    }

    /** Returns the number of ProductType entries in this type. */
    public int size() { return entries.size(); }

    /** Returns true iff this contains an entry of the given arity. */
    public boolean hasArity(int arity) { return arity>0 && arity<=MAXARITY && ((arities & (1<<arity)) != 0); }

    /**
     *      If all entries have the same arity, that arity is returned;
     * <br> else if some entries have different arities, we return -1;
     * <br> else we return 0 (which only happens when there are no entries at all).
     */
    public int arity() {
        if (arities==0) return 0;
        int ans=0;
        for(int i=1; i<=MAXARITY; i++) {
            if ((arities & (1<<i))!=0) { if (ans==0) ans=i; else return -1; }
        }
        return ans;
    }

    /**
     * Returns true if exists some A in this, some B in that, where (A[0]&B[0]!=empty)
     * <p> This method ignores the "is_int" and "is_bool" flags.
     */
    public boolean firstColumnOverlaps(Type that) {
        for (ProductType a:this)
            for (ProductType b:that)
              if (a.types[0].intersects(b.types[0]))
                return true;
        return false;
    }

    /**
     * Returns true if exists some A in this, some B in that, where (A.arity==B.arity, and A[0]&B[0]!=empty)
     * <p> This method ignores the "is_int" and "is_bool" flags.
     */
    public boolean canOverride(Type that) {
        if ((arities & that.arities)!=0)
          for (ProductType a:this)
            if ((that.arities & (1 << a.types.length))!=0)
              for (ProductType b:that)
                if (a.types.length==b.types.length && a.types[0].intersects(b.types[0]))
                   return true;
        return false;
    }

    /** Returns true iff exists some A in this, some B in that, where A.arity==B.arity */
    public boolean hasCommonArity(Type that) {
        return (arities & that.arities)!=0;
    }

    /**
     * Returns a new type { A->B | A is in this, and B is in that }
     *
     * <p>  ReturnValue.is_int  == false
     * <br> ReturnValue.is_bool == false
     *
     * <p> If this.size()==0, or that.size()==0, then result.size()==0
     *
     * @throws ErrorType if at least one A->B has arity > MAXARITY
     */
    public Type product(Type that) throws ErrorType {
        TempList<ProductType> ee=new TempList<ProductType>();
        int aa=0;
        for (ProductType a:this) for (ProductType b:that) aa=add(ee, aa, a.product(b));
        return make(false, false, ee.makeConst(), aa);
    }

    /**
     * Returns true iff { A&B | A is in this, and B is in that } can have tuples.
     */
    public boolean intersects(Type that) {
        if ((arities & that.arities)!=0)
          for (ProductType a:this) if (!a.isEmpty() && (that.arities & (1 << a.types.length))!=0)
             for (ProductType b:that) if (!b.isEmpty() && a.types.length==b.types.length && a.intersects(b))
                 return true;
        return false;
    }

    /**
     * Returns a new type { A&B | A is in this, and B is in that }
     *
     * <p>  ReturnValue.is_int  == false
     * <br> ReturnValue.is_bool == false
     *
     * <p> If this.size()==0, or that.size()==0, or they do not have entries with same arity, then result.size()==0
     */
    public Type intersect(Type that) {
        if ((arities & that.arities)==0) return EMPTY;
        TempList<ProductType> ee=new TempList<ProductType>();
        int aa=0;
        for (ProductType a:this) if ((that.arities & (1 << a.types.length))!=0)
          for (ProductType b:that) if (a.types.length==b.types.length)
            aa=add(ee, aa, a.intersect(b));
        return make(false, false, ee.makeConst(), aa);
    }

    /**
     * Returns a new type { A&that | A is in this }
     *
     * <p>  ReturnValue.is_int  == false
     * <br> ReturnValue.is_bool == false
     *
     * <p> If (this.size()==0), or (that.arity is not in this), then result.size()==0
     */
    public Type intersect(ProductType that) {
        if ((arities & (1 << that.types.length))==0) return EMPTY;
        TempList<ProductType> ee=new TempList<ProductType>();
        int aa=0;
        for (ProductType a:this) if (a.types.length==that.types.length) aa=add(ee, aa, a.intersect(that));
        return make(false, false, ee.makeConst(), aa);
    }

    /**
     * Returns a new type { A | A is in this, or A is in that }
     *
     * <p>  ReturnValue.is_int  == this.is_int  || that.is_int
     * <br> ReturnValue.is_bool == this.is_bool || that.is_bool
     *
     * <p> If this.size()==0 and that.size()==0, then result.size()==0
     *
     * <p> As a special guarantee: if that==null, then the merge() method just returns the current object
     */
    public Type merge(Type that) {
        if (that==null) return this;
        if (that.size()==0 && is_int==that.is_int && is_bool==that.is_bool) return this;
        TempList<ProductType> ee=new TempList<ProductType>(entries);
        int aa=arities;
        for(ProductType x:that) aa=add(ee,aa,x);
        return make(is_int||that.is_int, is_bool||that.is_bool, ee.makeConst(), aa);
    }

    /**
     * Returns a new type { A | A is in this, or A == that }
     *
     * <p>  ReturnValue.is_int  == this.is_int
     * <br> ReturnValue.is_bool == this.is_bool
     */
    public Type merge(ProductType that) {
        TempList<ProductType> ee=new TempList<ProductType>(entries);
        int aa=add(ee, arities, that);
        return make(is_int, is_bool, ee.makeConst(), aa);
    }

    /**
     * Returns a new type { A | A is in this, or A == that.subList(begin,end) }
     *
     * <p>  ReturnValue.is_int  == this.is_int
     * <br> ReturnValue.is_bool == this.is_bool
     *
     * @throws ErrorFatal if (0 <= begin < end <= that.arity) is not true
     */
    public Type merge(ProductType that, int begin, int end) throws ErrorFatal {
        if (!(0<=begin && begin<end && end<=that.types.length)) throw new ErrorFatal("Illegal index range.");
        PrimSig[] array = new PrimSig[end-begin];
        for(int i=0; i < array.length; i++) { array[i]=that.types[begin+i]; }
        TempList<ProductType> ee=new TempList<ProductType>(entries);
        int aa=add(ee, arities, new ProductType(array));
        return make(is_int, is_bool, ee.makeConst(), aa);
    }

    /**
     * Returns a new type { A | A is in this, or A == that }
     *
     * <p>  ReturnValue.is_int  == this.is_int
     * <br> ReturnValue.is_bool == this.is_bool
     *
     * @throws ErrorFatal if that.size()==0
     * @throws ErrorType if that.size()>MAXARITY
     */
    public Type merge(List<PrimSig> that) throws ErrorType, ErrorFatal {
        if (that.size() == 0) throw new ErrorFatal("Relation arity cannot be zero.");
        if (that.size() > MAXARITY) throw new ErrorType("Relation of arity > "+MAXARITY+" is unsupported.");
        PrimSig[] array=new PrimSig[that.size()];
        for(int i=0; i < array.length; i++) {
            array[i] = that.get(i);
            if (array[i]==Sig.NONE) {
                if ((arities & (1 << array.length))!=0) return this;
                for(i=0; i<array.length; i++) array[i]=Sig.NONE;
                break;
            }
        }
        TempList<ProductType> ee=new TempList<ProductType>(entries);
        int aa=add(ee, arities, new ProductType(array));
        return make(is_int, is_bool, ee.makeConst(), aa);
    }

    /**
     * Returns a new type { A | (A is in this && A.arity in that) or (A is in that && A.arity in this) }
     *
     * <p>  ReturnValue.is_int  == false
     * <br> ReturnValue.is_bool == false
     *
     * <p> If this.size()==0 or that.size()==0, then result.size()==0
     *
     * <p> Special promise: if the result would be identical to this, then we will return "this" as-is, without constructing a new object
     */
    public Type unionWithCommonArity(Type that) {
        if ((arities & that.arities)==0) return EMPTY;
        TempList<ProductType> ee=new TempList<ProductType>();
        int aa=0;
        if (this.size()>0 && that.size()>0) {
            for(ProductType x:this) {
                int ar = 1 << x.types.length;
                if ((that.arities & ar) != 0) { aa=(aa|ar); ee.add(x); }
                // This ensures the entries in "ee" will be in the same order as the entries in "this.entries"
            }
            for(ProductType x:that) {
                int ar = 1 << x.types.length;
                if ((this.arities & ar) != 0) aa=add(ee,aa,x);
                // add() ensures that if x doesn't need to change "ee", then "ee" will stay unchanged
            }
        }
        // So now, if nothing changed, we want to return "this" as-is
        if (!is_int && !is_bool && ee.size()==this.entries.size() && aa==this.arities) {
            for(int i=ee.size()-1; ; i--) {
                if (i<0) return this;
                if (ee.get(i) != this.entries.get(i)) break;
            }
        }
        return make(false, false, ee.makeConst(), aa);
    }

    /**
     * Returns a new type { A | (A is in this && A.arity in that) }
     *
     * <p>  ReturnValue.is_int  == false
     * <br> ReturnValue.is_bool == false
     *
     * <p> If this.size()==0 or that.size()==0, then result.size()==0
     */
    public Type pickCommonArity(Type that) {
        if (!is_int && !is_bool && (arities & that.arities)==arities) return this;
        TempList<ProductType> ee=new TempList<ProductType>();
        int aa=0;
        for(ProductType x:entries) {
            int xa = 1 << x.types.length;
            if ((that.arities & xa)!=0) { aa=(aa|xa); ee.add(x); }
        }
        return make(false, false, ee.makeConst(), aa);
    }

    /**
     * Returns a new type { A | A is binary and ~A is in this }
     *
     * <p>  ReturnValue.is_int  == false
     * <br> ReturnValue.is_bool == false
     *
     * <p> If this.size()==0, or does not contain any binary ProductType entries, then result.size()==0
     */
    public Type transpose() {
        if ((arities & (1<<2))==0) return EMPTY;
        TempList<ProductType> ee=new TempList<ProductType>();
        int aa=0;
        for(ProductType a:this) if (a.types.length==2) aa=add(ee, aa, a.transpose());
        return make(false, false, ee.makeConst(), aa);
    }

    /**
     * Returns a new type { A.B | exists A in this, exists B in that, where A.arity+B.arity>2 }
     * <p> If this.size()==0, or that.size()==0, or none of the entries have the right arity, then result.size()==0.
     *
     * <p>  ReturnValue.is_int  == false
     * <br> ReturnValue.is_bool == false
     *
     * @throws ErrorType if at least one combination A.B has arity > MAXARITY
     */
    public Type join(Type that) throws ErrorType {
        if (size()==0 || that.size()==0) return EMPTY;
        TempList<ProductType> ee=new TempList<ProductType>();
        int aa=0;
        for (ProductType a:this) for (ProductType b:that) if (a.types.length + b.types.length > 2) aa=add(ee, aa, a.join(b));
        return make(false, false, ee.makeConst(), aa);
    }

    /**
     * Returns a new type { R[0]->..->R[n-1] |
     * exists n-ary A in this, exists unary B in that, such that R[i]==A[i] except R[0]==(A[0] & B)
     *
     * <p>  ReturnValue.is_int  == false
     * <br> ReturnValue.is_bool == false
     *
     * <p> If this.size()==0, or that does not contain any unary entry, then result.size()==0
     */
    public Type domainRestrict(Type that) {
        TempList<ProductType> ee=new TempList<ProductType>();
        int aa=0;
        if (size()>0 && (that.arities & (1<<1))!=0) for (ProductType b:that)
          if (b.types.length==1) for (ProductType a:this)
            aa = add(ee, aa, a.columnRestrict(b.types[0], 0));
        return make(false, false, ee.makeConst(), aa);
    }

    /**
     * Returns a new type { R[0]->..->R[n-1] |
     * exists n-ary A in this, exists unary B in that, such that R[i]==A[i] except R[n-1]==(A[n-1] & B)
     *
     * <p>  ReturnValue.is_int  == false
     * <br> ReturnValue.is_bool == false
     *
     * <p> If this.size()==0, or that does not contain any unary entry, then result.size()==0
     */
    public Type rangeRestrict(Type that) {
        TempList<ProductType> ee=new TempList<ProductType>();
        int aa=0;
        if (size()>0 && (that.arities & (1<<1))!=0) for (ProductType b:that)
          if (b.types.length==1) for (ProductType a:this)
            aa = add(ee, aa, a.columnRestrict(b.types[0], a.types.length-1));
        return make(false, false, ee.makeConst(), aa);
    }

    /**
     * Returns a new type { A  |  (A in this) and (A.arity == arity) }
     *
     * <p>  ReturnValue.is_int  == false
     * <br> ReturnValue.is_bool == false
     *
     * <p> If it does not contain any entry with the given arity, then result.size()==0
     */
    public Type extract(int arity) {
        final int aa = (1<<arity);
        if (arity<=0 || arity>MAXARITY || (arities & aa)==0) return EMPTY;
        if (!is_bool && !is_int && arities==aa) return this;
        final TempList<ProductType> ee=new TempList<ProductType>();
        for(ProductType x:entries) if (x.types.length==arity) ee.add(x);
        return make(false, false, ee.makeConst(), aa);
    }

    /**
     * Returns a new type u + u.u + u.u.u + ... (where u == the set of binary entries in this type)
     *
     * <p>  ReturnValue.is_int  == false
     * <br> ReturnValue.is_bool == false
     *
     * <p> If it does not contain any binary entries, then result.size()==0
     */
    public Type closure() {
        try {
            Type ans=extract(2), u=ans, uu=u.join(u);
            while(uu.hasTuple()) {
                Type oldans=ans, olduu=uu;
                ans=ans.unionWithCommonArity(uu);
                uu=uu.join(u);
                if (oldans==ans && olduu.equals(uu)) break;
            }
            return ans;
        } catch(ErrorType ex) {
            return extract(2); // This is impossible, but we catch it any way
        }
    }

    /**
     * Merge "a" into the set of entries.
     *
     * <p>  If {a}+this.entries contain a set of entries X1..Xn, such that
     * <br>   (1) For each X:  X[j]==a[j] for i!=j, and X[i].super==a[i].super
     * <br>   (2) X1[i]..Xn[i] exhaust all the direct subsignatures of an abstract parent sig
     * <br> THEN:
     * <br>   we removeAll(X), then return the merged result of X1..Xn
     * <br> ELSE
     * <br>   we change nothing, and simply return null
     *
     * <p><b>Precondition:</b> a[i] is not "none", and a[i].parent is abstract, and a[i].parent!=UNIV
     */
    private static List<PrimSig> fold(ArrayList<List<PrimSig>> entries, List<PrimSig> a, int i) throws Err {
        PrimSig parent = a.get(i).parent;
        ArrayList<PrimSig> subs = new ArrayList<PrimSig>(parent.children());
        ArrayList<List<PrimSig>> ret = new ArrayList<List<PrimSig>>();
        for(int bi=entries.size()-1; bi>=0; bi--) {
            List<PrimSig> b=entries.get(bi);
            if (b.size() == a.size()) {
                for(int j=0; ;j++) {
                    if (j>=b.size()) {ret.add(b); subs.remove(b.get(i)); break;}
                    PrimSig bt1=a.get(j), bt2=b.get(j);
                    if (i==j && bt2.parent!=parent) break;
                    if (i!=j && bt2!=bt1) break;
                }
            }
        }
        subs.remove(a.get(i));
        if (subs.size()!=0) return null;
        entries.removeAll(ret);
        entries.remove(a);
        a=new ArrayList<PrimSig>(a);
        a.set(i, parent);
        return a;
    }

    /**
     * Return the result of folding this Type (that is, whenever a subset of relations are identical
     * except for 1 position, where together they comprise of all direct subsigs of an abstract sig,
     * then we merge them)
     *
     * <p> Note: the result is only current with respect to the current sig relations
     */
    public Iterable<List<PrimSig>> fold() {
        ArrayList<List<PrimSig>> e = new ArrayList<List<PrimSig>>();
        for(ProductType xx:entries) {
            List<PrimSig> x=Arrays.asList(xx.types);
            while(true) {
                int n=x.size();
                boolean changed=false;
                for(int i=0; i<n; i++) {
                    PrimSig bt=x.get(i);
                    if (bt.parent!=null && bt.parent!=UNIV && bt.parent.isAbstract) {
                        List<PrimSig> folded;
                        try { folded=fold(e,x,i); } catch(Err ex) { folded=null;}
                        if (folded!=null) {x=folded; changed=true; i--;}
                    }
                }
                if (changed==false) break;
            }
            e.add(x);
        }
        return e;
    }

    /** Returns a human-readable description of this type. */
    @Override public String toString() {
        boolean first=true;
        StringBuilder ans=new StringBuilder("{");
        if (is_int) { first=false; ans.append("PrimitiveInteger"); }
        if (is_bool) { if (!first) ans.append(", "); first=false; ans.append("PrimitiveBoolean"); }
        for(List<PrimSig> r:fold()) {
            if (!first) ans.append(", ");
            first=false;
            for(int i=0; i<r.size(); i++) { if (i!=0) ans.append("->"); ans.append(r.get(i)); }
        }
        return ans.append('}').toString();
    }
}