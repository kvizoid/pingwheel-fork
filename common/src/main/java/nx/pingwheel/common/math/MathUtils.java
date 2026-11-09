package nx.pingwheel.common.math;

import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import nx.pingwheel.common.render.WorldRenderContext;
import org.joml.Matrix3x2f;
import org.joml.Vector4f;

import static nx.pingwheel.common.CommonClient.Game;

public class MathUtils {
	private MathUtils() {}

	public static ScreenPos worldToScreen(Vec3 worldPos, WorldRenderContext ctx) {
		var window = Game.getWindow();
		var worldPosRel = new Vector4f(worldPos.subtract(ctx.camera.pos).toVector3f(), 1f);
		ctx.modelViewMatrix.transform(worldPosRel);
		ctx.projectionMatrix.transform(worldPosRel);

		var depth = worldPosRel.w;

		if (depth != 0) {
			worldPosRel.div(depth);
		}

		return new ScreenPos(
			window.getGuiScaledWidth() * (0.5f + worldPosRel.x * 0.5f),
			window.getGuiScaledHeight() * (0.5f - worldPosRel.y * 0.5f),
			depth
		);
	}

	public static void rotateZ(Matrix3x2f matrixStack, float theta) {
		matrixStack.mul(new Matrix3x2f().rotateLocal(theta));
	}

	public static Vec2 calculateAngleRectIntersection(float angle, Vec2 leftTop, Vec2 rightBottom) {
		var direction = new Vec3(Math.cos(angle), Math.sin(angle), 0f);
		var width = rightBottom.x - leftTop.x;
		var height = rightBottom.y - leftTop.y;
		direction = direction.multiply(new Vec3(1f / width, 1f / height, 0f));

		var dx = Math.cos(angle);
		var dy = Math.sin(angle);
		var cx = width * 0.5f;
		var cy = height * 0.5f;

		if (Math.abs(direction.x) < Math.abs(direction.y)) {
			if (direction.y < 0) {
				// top
				var t = -cy / dy;
				var x = cx + t * dx;

				return new Vec2((float)x + leftTop.x, leftTop.y);
			}

			// bottom
			var t = cy / dy;
			var x = cx + t * dx;

			return new Vec2((float)x + leftTop.x, rightBottom.y);
		}

		if (direction.x < 0) {
			// left
			var t = -cx / dx;
			var y = cy + t * dy;

			return new Vec2(leftTop.x, (float)y + leftTop.y);
		}

		// right
		var t = cx / dx;
		var y = cy + t * dy;

		return new Vec2(rightBottom.x, (float)y + leftTop.y);
	}

	private static final int CLIP_INSIDE = 0;
	private static final int CLIP_LEFT = 1;
	private static final int CLIP_RIGHT = 2;
	private static final int CLIP_TOP = 4;
	private static final int CLIP_BOTTOM = 8;

	private static int computeOutCode(float x, float y, float minX, float minY, float maxX, float maxY) {
		int code = CLIP_INSIDE;
		if (x < minX) code |= CLIP_LEFT;
		else if (x > maxX) code |= CLIP_RIGHT;
		if (y < minY) code |= CLIP_TOP;
		else if (y > maxY) code |= CLIP_BOTTOM;
		return code;
	}

	public static float[] clipLine(float x1, float y1, float x2, float y2, float minX, float minY, float maxX, float maxY) {
		int code1 = computeOutCode(x1, y1, minX, minY, maxX, maxY);
		int code2 = computeOutCode(x2, y2, minX, minY, maxX, maxY);
		boolean accept = false;

		int iterations = 0;
		while (iterations++ < 16) {
			if ((code1 | code2) == 0) {
				accept = true;
				break;
			} else if ((code1 & code2) != 0) {
				break;
			} else {
				float x = 0, y = 0;
				int codeOut = (code1 != 0) ? code1 : code2;

				if ((codeOut & CLIP_BOTTOM) != 0) {
					if (Math.abs(y2 - y1) < 1e-6f) break;
					x = x1 + (x2 - x1) * (maxY - y1) / (y2 - y1);
					y = maxY;
				} else if ((codeOut & CLIP_TOP) != 0) {
					if (Math.abs(y2 - y1) < 1e-6f) break;
					x = x1 + (x2 - x1) * (minY - y1) / (y2 - y1);
					y = minY;
				} else if ((codeOut & CLIP_RIGHT) != 0) {
					if (Math.abs(x2 - x1) < 1e-6f) break;
					y = y1 + (y2 - y1) * (maxX - x1) / (x2 - x1);
					x = maxX;
				} else if ((codeOut & CLIP_LEFT) != 0) {
					if (Math.abs(x2 - x1) < 1e-6f) break;
					y = y1 + (y2 - y1) * (minX - x1) / (x2 - x1);
					x = minX;
				}

				if (codeOut == code1) {
					x1 = x;
					y1 = y;
					code1 = computeOutCode(x1, y1, minX, minY, maxX, maxY);
				} else {
					x2 = x;
					y2 = y;
					code2 = computeOutCode(x2, y2, minX, minY, maxX, maxY);
				}
			}
		}

		if (accept && !Float.isNaN(x1) && !Float.isNaN(y1) && !Float.isNaN(x2) && !Float.isNaN(y2)) {
			return new float[] { x1, y1, x2, y2 };
		}
		return null;
	}
}
