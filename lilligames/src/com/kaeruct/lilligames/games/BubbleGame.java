package com.kaeruct.lilligames.games;

import java.util.Iterator;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.TimeUtils;
import com.kaeruct.lilligames.common.Particle;
import com.kaeruct.lilligames.screen.GameScreen;

public class BubbleGame extends MicroGame {
	
	Array<Particle> bubbles;
	Texture bubbleImage;
	Sound popSound;
	int interval = 700000000;
	long lastTime = 0;
	float minr = Gdx.graphics.getWidth()/32;
	float maxr = Gdx.graphics.getWidth()/10;
	float minspawn = maxr*2;
	float maxspawn = Gdx.graphics.getWidth()-maxr*2;
	int popped = 0;
	int poppedGoal;
	
	public BubbleGame(GameScreen parent) {
		super(parent);
		bubbles = new Array<Particle>();
		bubbleImage = new Texture(Gdx.files.internal("data/bubble.png"));
		popSound = Gdx.audio.newSound(Gdx.files.internal("data/pop.ogg"));
		bg = new Color(0.2f, 0.2f, 0.5f, 1.0f);
		poppedGoal = MathUtils.random(5, 20);
		
		parent.showMessage("POP "+poppedGoal+" !");
	}
	
	private void addBubble() {
		float r = MathUtils.random(minr, maxr),
			  x = MathUtils.random(minspawn-r*2, maxspawn+r*2),
		      y = -r;

		Particle bubble = new Particle(x, y, r, randomBrightColor());
		bubble.dy = -MathUtils.random(2, 5);
		bubbles.add(bubble);
	}
	@Override
	public void render() {
		super.render();
		
		camera.update();
		batch.setProjectionMatrix(camera.combined);
		batch.begin();

		for (Particle b : bubbles) {
			batch.setColor(b.color);
			batch.draw(bubbleImage, b.x-b.radius, b.y-b.radius, b.radius*2, b.radius*2);
	    }
		
		font.draw(batch, "Popped: "+popped, 0, font.getLineHeight());
		batch.end();
	}

	@Override
	public void update(float delta) {

		// generate bubbles
	    long t = TimeUtils.nanoTime();
	    if(t - lastTime > interval) {
		    addBubble();
		    lastTime = t;

		    if (interval > 200000000) {
		    	interval -= 5000000;
		    }
	    }
		
		boolean killed = false;
		Iterator<Particle> bb = bubbles.iterator(); 
		Particle bubble;

		// update bubbles
		while (bb.hasNext()) {
			bubble = bb.next();

	    	if (!bubble.update()) {
	    		bb.remove();
	    		continue;
	    	}
	    	
	    	bubble.dx = MathUtils.cos(bubble.oscillation += (0.0003 * bubble.radius));

	    	for (int i = 0; i < MULTITOUCH_COUNT && !killed; i++) {
				if (Gdx.input.isTouched(i) && Gdx.input.justTouched()) {
					touchPos.set(Gdx.input.getX(i), Gdx.input.getY(i), 0);
					camera.unproject(touchPos);

					if (bubble.contains(touchPos.x, touchPos.y)) {
						bubble.kill();
						killed = true;
						popped += 1;
						break;
					}
				}
	    	}
	    	
	    	if (bubble.offscreen()) {
	    		bubble.kill();
	    	}
		}
	}

	@Override
	public boolean isFinished() {
		return popped >= poppedGoal;
	}
	
	public void dispose() {
		popSound.dispose();
	}
}