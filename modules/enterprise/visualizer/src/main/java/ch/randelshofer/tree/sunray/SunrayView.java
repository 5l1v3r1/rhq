/*
 * @(#)SunrayView.java  1.0  September 18, 2007
 *
 * Copyright (c) 2007 Werner Randelshofer
 * Staldenmattweg 2, CH-6405 Immensee, Switzerland
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Werner Randelshofer. ("Confidential Information").  You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Werner Randelshofer.
 */

package ch.randelshofer.tree.sunray;

import ch.randelshofer.tree.NodeInfo;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.*;
import javax.swing.*;
/**
 * SunrayView.
 *
 *
 *
 * @author Werner Randelshofer
 * @version 1.0 September 18, 2007 Created.
 */
public class SunrayView extends JPanel implements SunrayViewer {
    private SunrayDraw draw;
    private SunrayDraw subDraw;
    private BufferedImage img;
    private boolean isInvalid;
    private Thread worker;
    private boolean drawHandles;
    private boolean isAdjusting;
    private boolean needsSimplify;
    /**
     * The selected node of the sunburst tree. Can be null.
     */
    private SunrayNode selectedNode;
    /**
     * The node under the mouse cursor. Can be null.
     */
    private SunrayNode hoverNode;

    private NodeInfo info;

    /** Creates new instance. */
    public SunrayView() {
        init();
    }

    public SunrayView(SunrayTree model) {
        this.draw = new SunrayDraw(model.getRoot(), model.getInfo());
        this.info = model.getInfo();
        init();
    }

    private void init() {
        initComponents();
        MouseHandler handler = new MouseHandler();
        addMouseListener(handler);
        addMouseMotionListener(handler);
        ToolTipManager.sharedInstance().registerComponent(this);
        //    setFont(new Font("Dialog", Font.PLAIN, 9));
    }

    private class MouseHandler implements MouseListener, MouseMotionListener {
        private double alphaStart;
        private boolean isMove;
        private Point moveStart;
        public void mouseClicked(MouseEvent evt) {
            SunrayNode node = draw.getNodeAt(evt.getX(), evt.getY());
            if (node == null && subDraw != null) {
                node = subDraw.getNodeAt(evt.getX(), evt.getY());
                if (node == subDraw.getRoot() && subDraw.getRoot().children().size() != 0) {
                    node = null;
                }
            }
            if (node == draw.getRoot()) {
                setSelectedNode(null);
                if (evt.getClickCount() == 2) {
                    setCenter(getWidth() / 2, getHeight() / 2);
                    setSelectedNode(null);
                }
            } else {
                setSelectedNode(node);
            }
            isInvalid = true;
            repaint();
        }

        public void mousePressed(MouseEvent e) {
            isMove = draw.getNodeAt(e.getX(), e.getY()) == draw.getRoot();
            moveStart = e.getPoint();
            alphaStart = draw.getTheta(e.getX(), e.getY());
        }

        public void mouseReleased(MouseEvent e) {
            if (drawHandles || isAdjusting) {
                drawHandles = false;
                if (isAdjusting) {
                    isAdjusting = false;
                    isInvalid = true;
                }
                repaint();
            }
        }

        public void mouseEntered(MouseEvent e) {
        }

        public void mouseExited(MouseEvent e) {
            if (drawHandles) {
                drawHandles = false;
                repaint();
            }
        }

        public void mouseDragged(MouseEvent e) {
            isAdjusting = true;
            if (isMove) {
                Point moveNow = e.getPoint();
                int cx = (int) draw.getCX();
                int cy = (int) draw.getCY();
                setCenter(cx + moveNow.x - moveStart.x,
                        cy + moveNow.y - moveStart.y
                        );
                moveStart = moveNow;
                isInvalid = true;
                repaint();
            } else {
                double alphaNow = draw.getTheta(e.getX(), e.getY());
                setRotation(draw.getRotation() - alphaNow + alphaStart);
                isInvalid = true;
                repaint();
            }
        }

        public void mouseMoved(MouseEvent e) {
            hoverNode = draw.getNodeAt(e.getX(), e.getY());
            if (hoverNode == null && subDraw != null) {
                hoverNode = subDraw.getNodeAt(e.getX(), e.getY());
                if (hoverNode == subDraw.getRoot() &&
                        subDraw.getRoot().children().size() != 0) {
                    hoverNode = null;
                }
            }
            //isInvalid = true;
            repaint();
            /*
            boolean b = draw.getNodeAt(e.getX(), e.getY()) == draw.getRoot();
            if (b != drawHandles) {
                drawHandles = b;
                repaint();
            }*/
        }
    }

    private void setRotation(double rotation) {
                draw.setRotation(rotation);
                if (subDraw != null) {
                    subDraw.setRotation(rotation);
                }
    }

    private void setCenter(double cx, double cy) {
        draw.setCX(cx);
        draw.setCY(cy);
        if (subDraw != null) {
            subDraw.setCX(cx);
            subDraw.setCY(cy);
        }
    }
    private void setOuterRadius(double r) {
        if (subDraw != null) {
            draw.setOuterRadius(r / 2 - (r / 2) / draw.getTotalDepth());
            subDraw.setOuterRadius(r);
            if (subDraw.getRoot().children().size() == 0) {
                subDraw.setInnerRadius(r / 2);
            } else {
                subDraw.setInnerRadius(r / 2 - (r / 2) / draw.getTotalDepth());
            }
        } else {
            draw.setOuterRadius(r);
        }
    }



    public void paintComponent(Graphics gr) {
        int w = getWidth();
        int h = getHeight();

        if (img == null ||
                img.getWidth() != w ||
                img.getHeight() != h) {
            if (img == null) {
                setCenter((double) w / 2, (double) h / 2);
                setOuterRadius(Math.min(w, h) / 2 - 4);
            } else {
                setCenter(draw.getCX() / img.getWidth() * w,
                        draw.getCY() / img.getHeight() * h);
                setOuterRadius(Math.min(w, h) / 2 - 4);
            }
            img = null;
            img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            isInvalid = true;
        }
        if (isInvalid) {
            isInvalid = false;
            Graphics2D g = img.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setBackground(Color.WHITE);
            g.setFont(getFont());
            g.clearRect(0, 0, img.getWidth(), img.getHeight());
            if (isAdjusting && needsSimplify) {
                draw.drawContours(g, draw.getRoot(), Color.gray);
                if (subDraw != null) {
                subDraw.drawContours(g, subDraw.getRoot(), Color.gray);
                }
            } else {
                long start = System.currentTimeMillis();
                draw.drawTree(g);
                if (subDraw != null) {
                    if (subDraw.getRoot().children().size() == 0) {
                        subDraw.drawTree(g);
                    } else {
                        subDraw.drawDescendants(g, subDraw.getRoot());
                    }
                }
                long end = System.currentTimeMillis();
                needsSimplify = (end - start) > 500;
            }

            g.dispose();
        }


        if (worker == null) {
            gr.drawImage(img, 0, 0, this);
        }
        if (selectedNode != null) {
            Graphics2D g = (Graphics2D) gr;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            if (selectedNode.children().size() == 0) {
                draw.drawSubtreeBounds(g, selectedNode, Color.red);
            } else {
                draw.drawDescendantSubtreeBounds(g, selectedNode, Color.red);
            }
        }
        if (hoverNode != null) {
            Graphics2D g = (Graphics2D) gr;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            draw.drawNodeBounds(g, hoverNode, Color.black);
            if (subDraw != null && subDraw.getRoot().isDescendant(hoverNode)) {
                if (hoverNode != subDraw.getRoot() || subDraw.getRoot().children().size() == 0) {
                subDraw.drawNodeBounds(g, hoverNode, Color.BLACK);
                }
            }
        }

        if (drawHandles) {
            Graphics2D g = (Graphics2D) gr;
            double cx = draw.getCX();
            double cy = draw.getCY();
            g.setColor(Color.BLACK);
            AffineTransform t = new AffineTransform();
            t.translate(cx, cy);
            t.rotate(draw.getRotation() * Math.PI / -180d);
            AffineTransform oldT = (AffineTransform) g.getTransform().clone();
            g.setTransform(t);
            g.draw(new Line2D.Double(-5, 0, 5, 0));
            g.draw(new Line2D.Double(0, -5, 0, 5));
            g.setTransform(oldT);
        }

    }

    /**
     * Returns the tooltip to be displayed.
     *
     * @param event    the event triggering the tooltip
     * @return         the String to be displayed
     */
    public String getToolTipText(MouseEvent event) {
        int x = event.getX();
        int y = event.getY();

        SunrayNode node = draw.getNodeAt(x, y);
        if (node == null && subDraw != null) {
            node = subDraw.getNodeAt(x, y);
            if (node == subDraw.getRoot() && subDraw.getRoot().children().size() != 0) {
                node = null;
            }
        }
            return (node == null) ? null : info.getTooltip(node.getDataNodePath());
    }

    public void setSelectedNode(SunrayNode newValue) {
        selectedNode = newValue;
        if (selectedNode == null) {
            if (subDraw != null) {
                draw.setOuterRadius(subDraw.getOuterRadius());
                subDraw = null;
            }
        } else {
            double outerRadius = (subDraw == null) ? draw.getOuterRadius() : subDraw.getOuterRadius();

            if (selectedNode.children().size() == 0) {
                subDraw = new SunrayDraw(selectedNode, draw.getInfo());
            } else {
                subDraw = new SunrayDraw(selectedNode, draw.getInfo());
            }
            setRotation(draw.getRotation());
                setOuterRadius(outerRadius);
                setCenter(draw.getCX(), draw.getCY());
        }
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents() {

        setLayout(new java.awt.BorderLayout());

    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables

}
