package com.kaeruct.lilligames.games;

import java.util.Iterator;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.TimeUtils;
import com.kaeruct.lilligames.common.Particle;
import com.kaeruct.lilligames.screen.GameScreen;

public class FlickGame extends MicroGame {
	Array<Particle> objects;
	
	TextureAtlas texAtlas;
	Array<AtlasRegion> atlasRegions;
	
	Vector3 lastTouchPos;
	int spawnInterval = 250000000;
	int touchDragCount = 0;
	int flickId = 0;
	int misses = 0;
	long lastTime = 0;
	
	class InputHandler extends InputAdapter {
		@Override
		public boolean touchDown(int screenX, int screenY, int pointer, int button) {
			touchDragCount = 0;
			
			lastTouchPos.set(screenX, screenY, 0);
			camera.unproject(lastTouchPos);
			return true;
		}
		
		@Override
		public boolean touchDragged (int screenX, int screenY, int pointer) {
			touchPos.set(screenX, screenY, 0);
			camera.unproject(touchPos);
			
			// buffer a few drag events to get a better line sample
			touchDragCount++;
			if (touchDragCount < 3) {
				return true;
			}
			touchDragCount = 0;
			
			Vector2 center = new Vector2();
			Vector2 touchPos2 = new Vector2(touchPos.x, touchPos.y);
			Vector2 lastTouchPos2 = new Vector2(lastTouchPos.x, lastTouchPos.y);
			Vector2 velocity = new Vector2();
			float displacement = 0.0f;
			
			//System.out.printf("touch: %f %f last: %f %f\n", touchPos.x, touchPos.y, lastTouchPos.x, lastTouchPos.y);
			
			for (Particle o : objects) {
				center.set(o.x, o.y);
				displacement = Intersector.intersectSegmentCircleDisplace(lastTouchPos2, touchPos2, center, o.radius, velocity);
				if (displacement != Float.POSITIVE_INFINITY) {
					velocity = lastTouchPos2.sub(touchPos2).mul(0.5f);
					o.dx += -velocity.x;
					o.dy += velocity.y;
					break;
				}
			}
			
			// touchPos is already in world space
			lastTouchPos.set(touchPos);
			return true;
		}
		
		@Override
		public boolean touchUp(int screenX, int screenY, int pointer, int button) {
			touchDragCount = 0;
			
			lastTouchPos.set(screenX, screenY, 0);
			camera.unproject(lastTouchPos);
			return true;
		}
	}
	
	public FlickGame (GameScreen parent) {
		super(parent);
		bg = new Color(0.78f, 0.88f, 0.92f, 1.0f);
		
		texAtlas = new TextureAtlas(Gdx.files.internal("data/fruit.atlas"));
		atlasRegions = texAtlas.getRegions();
		
		objects = new Array<Particle>();
		lastTouchPos = new Vector3();
		
		Gdx.input.setInputProcessor(new InputHandler());
		
		// random fruit
		flickId = MathUtils.random(0, atlasRegions.size - 1);
		parent.showMessage("Hit away the " + atlasRegions.get(flickId).name + "!");
	}
	
	private void spawnObject (int objectId) {
		Particle p = new Particle();
		p.x = -10.0f;
		p.y = Gdx.graphics.getHeight() / (MathUtils.random(3, 12) * 0.5f);
		p.radius = 40.0f;
		p.dx += 6.0f;
		p.misc = objectId;
		objects.add(p);
	}
	
	private boolean isCaptured (Particle o) {
		return o.x > Gdx.graphics.getWidth() + o.radius * 2;
	}
	
	// whether the given object is off-screen but NOT in the capture zone
	private boolean isUncaptured (Particle o) {
		return o.offscreen() && !isCaptured(o);
	}
	
	@Override
	public void onRender() {
		for (Particle o : objects) {
			batch.draw(atlasRegions.get((int)o.misc),
					o.x-o.radius, o.y-o.radius,
					o.radius, o.radius,
					o.radius*2, o.radius*2,
					1.0f, 1.0f, o.rotation);
	    }
		
	}
	
	@Override
	public void update(float delta) {
		long t = TimeUtils.nanoTime();
		if (t - lastTime > spawnInterval) {
			lastTime = t;
			if (MathUtils.random() < 0.4) {
				spawnObject(flickId);
			} else {
				spawnObject(MathUtils.random(0, atlasRegions.size - 1));
			}
		}
		
		Iterator<Particle> bb = objects.iterator(); 
		Particle o;

		while (bb.hasNext()) {
			o = bb.next();
			
	    	if (!o.update()) {
	    		bb.remove();
	    		continue;
	    	}
	    	
	    	// flickables entering the "capture zone"
	    	// OR good objects flying off-screen
	    	// offscreen to prevent excessive miss tallying
	    	if (o.misc == flickId && isCaptured(o) || o.misc != flickId && isUncaptured(o)) {
	    		misses++;
	    		parent.showMessage("Misses: " + misses + "!");	    		

	    		if (misses > 2) {
	    			lost = true;
	    		}
	    	}
	    	
	    	
	    	if (o.offscreen()) {
	    		o.kill();
	    	}
		}
	}
	
	@Override
	public boolean isFinished() {
		return misses < 3;
	}
	
	@Override
	public void dispose() {
		super.dispose();
		texAtlas.dispose();
		Gdx.input.setInputProcessor(null);
	}
}
