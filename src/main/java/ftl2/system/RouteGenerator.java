package ftl2.system;

import ftl2.math.ConstFPoint;
import ftl2.math.ConstPoint;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility to compute a smooth curved route (quadratic Bezier) between two POIs
 * that attempts to arc away from the central star.
 */
public class RouteGenerator {
    /**
     * Generate a list of waypoints in system-local coordinates between start and end.
     */
    public static List<ConstFPoint> generateRoute(ConstPoint start, ConstPoint end, System system, int numPoints) {
        float sx = start.getX();
        float sy = start.getY();
        float ex = end.getX();
        float ey = end.getY();

        // Midpoint
        float mx = (sx + ex) / 2f;
        float my = (sy + ey) / 2f;

        // Star position (usually 0,0)
        float starX = (float) system.getCentre().getPos().getX();
        float starY = (float) system.getCentre().getPos().getY();

        // Vector from star to midpoint
        float vx = mx - starX;
        float vy = my - starY;
        double vlen = Math.hypot(vx, vy);

        // Base offset that pushes the control point away from the star
        float segLen = (float) Math.hypot(ex - sx, ey - sy);
        float baseOffset = Math.max(120f, segLen * 0.5f);

        // Compute two candidate control points (both sides of midpoint) and pick the one
        // with the larger minimum clearance along the curve.
        float cx1, cy1, cx2, cy2;

        // Candidate 1: push away from star (if direction available), otherwise perpendicular
        if (vlen < 1e-3) {
            float lx = ex - sx;
            float ly = ey - sy;
            float px = -ly;
            float py = lx;
            double plen = Math.hypot(px, py);
            if (plen < 1e-3) {
                cx1 = mx;
                cy1 = my;
            } else {
                cx1 = mx + px / (float) plen * baseOffset;
                cy1 = my + py / (float) plen * baseOffset;
            }
        } else {
            float dx = vx / (float) vlen;
            float dy = vy / (float) vlen;
            cx1 = mx + dx * baseOffset;
            cy1 = my + dy * baseOffset;
        }

        // Candidate 2: opposite direction from candidate 1 (flip around midpoint)
        cx2 = mx - (cx1 - mx);
        cy2 = my - (cy1 - my);

        // Helper to compute minimum clearance from star along bezier defined by control point
        java.util.function.BiFunction<Float, Float, Double> computeMinClearance = (cxx, cyy) -> {
            double minD = Double.MAX_VALUE;
            for (int i = 0; i <= numPoints; i++) {
                float t = i / (float) numPoints;
                float omt = 1 - t;
                float bx = omt * omt * sx + 2 * omt * t * cxx + t * t * ex;
                float by = omt * omt * sy + 2 * omt * t * cyy + t * t * ey;
                double d = Math.hypot(bx - starX, by - starY);
                if (d < minD) minD = d;
            }
            return minD;
        };

        double clear1 = computeMinClearance.apply(cx1, cy1);
        double clear2 = computeMinClearance.apply(cx2, cy2);

        float cx, cy;
        if (clear2 > clear1) {
            cx = cx2; cy = cy2;
        } else {
            cx = cx1; cy = cy1;
        }

        // Iteratively ensure clearance from star: if any sample is too close, increase offset
        float minClearance = 80f;
        int attempts = 0;
        while (attempts < 6) {
            boolean ok = true;
            for (int i = 0; i <= numPoints; i++) {
                float t = i / (float) numPoints;
                float omt = 1 - t;
                float bx = omt * omt * sx + 2 * omt * t * cx + t * t * ex;
                float by = omt * omt * sy + 2 * omt * t * cy + t * t * ey;
                double d = Math.hypot(bx - starX, by - starY);
                if (d < minClearance) { ok = false; break; }
            }
            if (ok) break;
            // increase offset and recompute control point from midpoint direction
            baseOffset *= 1.5f;
            float dirx = mx - starX;
            float diry = my - starY;
            double dlen = Math.hypot(dirx, diry);
            if (dlen < 1e-3) {
                // perpendicular fallback
                float lx = ex - sx;
                float ly = ey - sy;
                float px = -ly;
                float py = lx;
                double plen = Math.hypot(px, py);
                if (plen >= 1e-3) {
                    cx = mx + px / (float) plen * baseOffset;
                    cy = my + py / (float) plen * baseOffset;
                }
            } else {
                cx = mx + (dirx / (float) dlen) * baseOffset;
                cy = my + (diry / (float) dlen) * baseOffset;
            }
            attempts++;
        }

        ArrayList<ConstFPoint> pts = new ArrayList<>();
        for (int i = 0; i <= numPoints; i++) {
            float t = i / (float) numPoints;
            float omt = 1 - t;
            float bx = omt * omt * sx + 2 * omt * t * cx + t * t * ex;
            float by = omt * omt * sy + 2 * omt * t * cy + t * t * ey;
            pts.add(new ConstFPoint(bx, by));
        }

        return pts;
    }
}
