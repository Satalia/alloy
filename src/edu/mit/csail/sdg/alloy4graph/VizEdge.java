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

package edu.mit.csail.sdg.alloy4graph;

import static java.awt.Color.BLACK;
import java.awt.Color;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.QuadCurve2D;
import java.awt.geom.Rectangle2D;
import static java.lang.StrictMath.PI;
import static java.lang.StrictMath.sin;
import static java.lang.StrictMath.cos;
import static java.lang.StrictMath.atan2;
import static java.lang.StrictMath.toRadians;

/**
 * Mutable; represents a graphical edge.
 *
 * <p><b>Thread Safety:</b> Can be called only by the AWT event thread.
 */

public final strictfp class VizEdge extends DiGraph.DiEdge {

    // =============================== adjustable options ==================================================

    // /** The color to use for highlighted edge. */
    // private static Color color1 = Color.RED;

    // /** The color to use for other edges that are in the same group as the highlighted edge. */
    // private static Color color2 = new Color(255,120,120);

    /** This determines the font size. */
    static final int fontSize = 12;

    /** This determines the minimum width of a self loop. */
    static final int selfLoopMinWidth = 20;

    /** This determines how much farther to the right you need to go, for each subsequent self loop on the same node. */
    static final int selfLoopXGap = 10;

    /** This determines the prefered vertical gap between self loops. */
    static final int selfLoopYGap = 10;

    // =============================== per-edge settings ===================================================

    /** a user-provided annotation that will be associated with this edge (can be null) */
    public final Object uuid;

    /** The label; can be an empty string if there is no label; NOTE: label is only shown if the start node is not a dummy node. */
    private String label = "";

    /** The location and size of the label box (if it's been calculated) */
    private AvailableSpace.Box labelbox = new AvailableSpace.Box();

    /** The group that this edge belongs to. */
    final Object group;

    /** Return the X coordinate of the top-left corner of the label box. */
    public int getLabelX() { return labelbox.x; }

    /** Return the Y coordinate of the top-left corner of the label box. */
    public int getLabelY() { return labelbox.y; }

    /** Return the width of the label box. */
    public int getLabelW() { return labelbox.w; }

    /** Return the height of the label box. */
    public int getLabelH() { return labelbox.h; }

    /** Whether to draw an arrow head on the "from" node; default is false. */
    private boolean ahead = false;

    /** Whether to draw an arrow head on the "to" node; default is true. */
    private boolean bhead = true;

    /** The color of the edge; default is black; never null. */
    private Color color = BLACK;

    /** The line-style of the edge; default is SOLID; never null. */
    private VizStyle style = VizStyle.SOLID;

    /** The edge weight; always between 1 and 10000 inclusively. */
    private int weight = 1;

    /** The actual path corresponding to this edge; null if we have not assigned the path yet. */
    private VizPath path = null;

    /** Returns the group that this VizEdge belongs to. */
    public Object group() { return group; }

    /** Returns the edge weight (which is always between 1 and 10000 inclusively). */
    public int weight() { return weight; }

    /** Returns the line style; never null. */
    public VizStyle style() { return style; }

    /** Returns the line color; never null. */
    public Color color() { return color; }

    /** Returns true if we will draw an arrow head on the "from" node. */
    public boolean ahead() { return ahead; }

    /** Returns true if we will draw an arrow head on the "to" node. */
    public boolean bhead() { return bhead; }

    /** Returns the label on this edge. */
    public String label() { return label; }

    /** Sets the edge weight between 1 and 10000. */
    public VizEdge set(int weightBetween1And10000) {
        if (weightBetween1And10000<1) weightBetween1And10000=1;
        if (weightBetween1And10000>10000) weightBetween1And10000=10000;
        weight=weightBetween1And10000;
        return this;
    }

    /** Sets whether we will draw an arrow head on the "from" node, and whether we will draw an arrow head on the "to" node. */
    public VizEdge set(boolean from, boolean to) {
        this.ahead=from;
        this.bhead=to;
        return this;
    }

    /** Sets the line style. */
    public VizEdge set(VizStyle style) {
        if (style!=null) this.style=style;
        return this;
    }

    /** Sets the line color. */
    public VizEdge set(Color color) {
        if (color!=null) this.color=color;
        return this;
    }

    /** Returns a String representing this edge. */
    @Override public String toString() {
        return "Edge " + a() + (ahead?"<--":"---") + label + (bhead?"-->":"---") + b();
    }

    /** Construct an edge from "from" to "to" with the given arrow head settings, then add the edge to the graph. */
    public VizEdge(VizNode from, VizNode to, Object uuid, String label, boolean drawArrowHeadOnFrom, boolean drawArrowHeadOnTo, VizStyle style, Color color, Object group) {
       super(from, to); // The parent's constructor will add the edge A->B to the graph
       this.uuid = uuid;
       this.group = (group==null) ? this : group;
       this.label=label;
       this.ahead=drawArrowHeadOnFrom;
       this.bhead=drawArrowHeadOnTo;
       if (style!=null) this.style=style;
       if (color!=null) this.color=color;
       if (label.length()>0) {
           Rectangle2D box = Artist.getStringBounds(fontSize, style==VizStyle.BOLD, label);
           labelbox.x = 0;
           labelbox.y = 0;
           labelbox.w = (int) box.getWidth();
           labelbox.h = (int) box.getHeight();
       }
    }

    /** Construct an edge from "from" to "to" with the default arrow head settings, then add the edge to the graph. */
    public VizEdge(VizNode from, VizNode to, Object uuid, String label, Object group) {
       this(from, to, uuid, label, false, true, null, null, group);
    }

    /** Reset the path as a straightline from the center of the "from" node to the center of the "to" node. */
    void resetPath() {
        VizNode a=a(), b=b();
        double ax=a.x(), ay=a.y();
        if (a==b) {
           int i, n=a.selfEdges().size(), q=selfLoopMinWidth, d=selfLoopXGap;
           for(i=0; i<n; i++) if (a.selfEdges().get(i)==this) break;
           double p=a.getHeight()/(2*n+1D);
           if (!(p<=selfLoopYGap)) p=selfLoopYGap;
           p=i*p+(p/2D);
           path=new VizPath(ax, ay-p, ax, ay+p);
           path.add(1, ax+a.getWidth()/2+q+i*d, ay-p);
           path.add(2, ax+a.getWidth()/2+q+i*d, ay+p);
        } else {
           int i=0, n=0;
           for(VizEdge e:a.outEdges()) {
               if (e==this) i=n;
               if (e.b()==b) n++;
           }
           double cx=b.x(), cy=b.y(), bx=(ax+cx)/2, by=(ay+cy)/2;
           path=new VizPath(ax, ay, cx, cy);
           if (n>1) {
               if (i<n/2) bx=bx-(n/2-i)*10; else bx=bx+(i-n/2+1)*10; // the spacing between edges that share the same endpoints
               path.add(1, bx, by);
           }
        }
    }

    /** Given that this edge is already well-laidout, this method moves the label hoping to avoid/minimize overlap. */
    void repositionLabel(AvailableSpace sp) {
        if (label.length()==0) return;
        if (a()==b()) return; // self edge
        int gap = style==VizStyle.BOLD ? 3 : 0; // If the line is bold, we need to shift the label to the right a little bit
        int ay=a().y()+a().getHeight()/2, by=b().y()-b().getHeight()/2, midy=(ay+by)/2;
        if (b().shape()==null) midy=by-labelbox.h;
        for(int gp=0; ; gp=gp+2) {
            boolean done = true;
            int y = midy-gp;
            if (y>ay && y<by) {
                done = false;
                int xpre = (int) (path.intersectsHorizontal(y-5));
                int xpost = (int) (path.intersectsHorizontal(y+5));
                int x = (int) (path.intersectsHorizontal(xpre>=xpost ? y : y+labelbox.h)) + gap;
                if (sp.ok(x, y, labelbox.w, labelbox.h)) { sp.add(x, y, labelbox.w, labelbox.h); labelbox.x=x; labelbox.y=y; return; }
            }
            y = midy+gp;
            if (y>ay && y<by) {
                done = false;
                int xpre = (int) (path.intersectsHorizontal(y-5));
                int xpost = (int) (path.intersectsHorizontal(y+5));
                int x = (int) (path.intersectsHorizontal(xpre>=xpost ? y : y+labelbox.h)) + gap;
                if (sp.ok(x, y, labelbox.w, labelbox.h)) { sp.add(x, y, labelbox.w, labelbox.h); labelbox.x=x; labelbox.y=y; return; }
            }
            if (done) break;
        }
        int y = ay+(by-ay)/2;
        int xpre = (int) (path.intersectsHorizontal(y-5));
        int xpost = (int) (path.intersectsHorizontal(y+5));
        int realY= (xpre>=xpost) ? y : (y+labelbox.h);
        int x = (int) path.intersectsHorizontal(realY) + gap;
        labelbox.x = (int)x;
        labelbox.y = (int)y;
        sp.add(labelbox.x, labelbox.y, labelbox.w, labelbox.h);
    }

    /** Returns the current path; if the path was not yet assigned, it returns a straight line from "from" node to "to" node. */
    VizPath path() {
        if (path==null) resetPath();
        return path;
    }

    /** Add the given (x,y) point into the path at the i-th position in the path (where i counts from 0...) */
    void pathAdd(int i, double x, double y) {
        if (path==null) resetPath();
        path.add(i,x,y);
    }

    /** Returns true iff the edge intersects the given point (px,py), given the current zoom scale. */
    public boolean intersects(double px, double py, double scale) {
        double fudge=10/scale; // we enlarge (px,py) into a square of size (fudge*2) x (fudge*2) when testing for intersection
        return path.intersectsVertical(px, py-fudge, py+fudge, null)>=0 || path.intersectsHorizontal(px-fudge, px+fudge, py);
    }

    /** Assuming this edge's coordinates have been assigned, and given the current zoom scale, draw the edge. */
    public void draw(Artist gr, double scale, VizEdge highlight) {
       final int top = ((VizGraph)(a().graph)).getTop(), left=((VizGraph)(a().graph)).getLeft();
       gr.translate(-left, -top);
       if (highlight==this) { gr.setColor(color); gr.set(VizStyle.SELECTED, scale); }
          else if (highlight!=null && highlight.group==group) { gr.setColor(color); gr.set(VizStyle.SELECTED, scale); }
          else if (highlight!=null) { gr.setColor(Color.BLACK); gr.set(/*style!=VizStyle.BOLD ? style :*/ VizStyle.SOLID, scale); }
          else { gr.setColor(color); gr.set(style, scale); }
       if (a()==b()) {
          // Draw the self edge
          double x0=path.getX(0), y0=path.getY(0), x1=path.getX(1), y1=y0, x2=x1, y2=path.getY(2), x3=path.getX(3), y3=y2;
          double gap=(y2-y1)/3; if (!(gap<5D)) gap=5D;
          gr.draw(new Line2D.Double(x0, y0, x1-5, y1), false);
          gr.draw(new QuadCurve2D.Double(x1-5, y1, x1, y1, x1, y1+gap), false);
          gr.draw(new Line2D.Double(x1, y1+gap, x2, y2-gap), false);
          gr.draw(new QuadCurve2D.Double(x2, y2-gap, x2, y2, x2-5, y2), false);
          gr.draw(new Line2D.Double(x2-5, y2, x3, y3), false);
       } else {
          // Concatenate this path and its connected segments into a single VizPath object, then draw it
          VizPath p=null;
          VizEdge e=this;
          while(e.a().shape()==null) e=e.a().inEdges().get(0); // Let e be the first segment of this chain of connected segments
          while(true) {
             p = (p==null) ? e.path : new VizPath(p, e.path);
             if (e.b().shape()!=null) break;
             e = e.b().outEdges().get(0);
          }
          p.draw(gr);
       }
       gr.set(VizStyle.SOLID, scale);
       gr.translate(left, top);
       //if (highlight==null && label.length()>0) drawLabel(gr, color, null);
    }

    /** Draw the edge label using the given color. */
    void drawLabel(Artist gr, Color color, Color erase) {
        if (label.length()>0) {
            final int top = ((VizGraph)(a().graph)).getTop(), left=((VizGraph)(a().graph)).getLeft();
            int x, y, n = path.getPoints();
            if ((n&1)==0) x=(int)(path.getX(n/2-1)+path.getX(n/2))/2; else x=(int)path.getX(n/2);
            if ((n&1)==0) y=(int)(path.getY(n/2-1)+path.getY(n/2))/2; else y=(int)path.getY(n/2);
            x=x-labelbox.w/2-3;
            y=y-labelbox.h/2-3;
            gr.translate(-left, -top);
            if (erase!=null) {
               Rectangle2D.Double rect = new Rectangle2D.Double(x, y, labelbox.w+6, labelbox.h+6);
               gr.setColor(erase); gr.draw(rect, true);
               gr.setColor(color); gr.draw(rect, false);
            }
            gr.drawString(label, x+3, y+3+Artist.getMaxAscent(fontSize, false));
            gr.translate(left, top);
        }
    }

    /** Assuming this edge's coordinates have been assigned, and given the current zoom scale, draw the arrow heads if any. */
    public void drawArrowhead(Artist gr, double scale, VizEdge highlight, double tipLength) {
       final int top=((VizGraph)(a().graph)).getTop(), left=((VizGraph)(a().graph)).getLeft();
       // Check to see if this edge is highlighted or not
       double fan;
       if (highlight==this) {
          fan=selectedFan; gr.setColor(color); gr.set(VizStyle.SELECTED, scale);
       } else if (highlight!=null && highlight.group==group) {
          fan=selectedFan; gr.setColor(color); gr.set(VizStyle.SELECTED, scale);
       } else if (highlight!=null) {
          fan=smallFan; gr.setColor(Color.BLACK); gr.set(VizStyle.SOLID /*style==VizStyle.BOLD ? VizStyle.SOLID : style*/, scale);
       } else {
          fan=(style==VizStyle.BOLD ? bigFan : smallFan); gr.setColor(color); gr.set(style, scale);
       }
       for(VizEdge e=this; ;e=e.b().outEdges().get(0)) {
          if ((e.ahead && e.a().shape()!=null) || (e.bhead && e.b().shape()!=null)) {
             int n = e.path.getPoints();
             if (e.ahead && e.a().shape()!=null) {
                double ax = e.path.getX(0), ay=e.path.getY(0), bx=e.path.getX(1), by=e.path.getY(1);
                double t = PI + atan2(ay-by, ax-bx);
                double gx1 = ax + tipLength*cos(t-fan), gy1 = ay + tipLength*sin(t-fan);
                double gx2 = ax + tipLength*cos(t+fan), gy2 = ay + tipLength*sin(t+fan);
                GeneralPath gp=new GeneralPath();
                gp.moveTo((float)(gx1-left), (float)(gy1-top)); gp.lineTo((float)(ax-left), (float)(ay-top));
                gp.lineTo((float)(gx2-left), (float)(gy2-top)); gp.closePath(); gr.draw(gp,true);
             }
             if (e.bhead && e.b().shape()!=null) {
                double ax = e.path.getX(n-2), ay=e.path.getY(n-2), bx=e.path.getX(n-1), by=e.path.getY(n-1);
                double t = PI + atan2(by-ay, bx-ax);
                double gx1 = bx + tipLength*cos(t-fan), gy1 = by + tipLength*sin(t-fan);
                double gx2 = bx + tipLength*cos(t+fan), gy2 = by + tipLength*sin(t+fan);
                GeneralPath gp=new GeneralPath();
                gp.moveTo((float)(gx1-left), (float)(gy1-top)); gp.lineTo((float)(bx-left), (float)(by-top));
                gp.lineTo((float)(gx2-left), (float)(gy2-top)); gp.closePath(); gr.draw(gp,true);
             }
          }
          if (e.b().shape()!=null) break;
       }
    }

    /** The angle (in radian) to fan out the arrow head, if the line is not bold. */
    private final double smallFan = toRadians(16);

    /** The angle (in radian) to fan out the arrow head, if the line is bold. */
    private final double bigFan = toRadians(32);

    /** The angle (in radian) to fan out the arrow head, if the line is selected. */
    private final double selectedFan = toRadians(48);
}
