package ca.jvsh.fallingdroids;

import static org.anddev.andengine.extension.physics.box2d.util.constants.PhysicsConstants.PIXEL_TO_METER_RATIO_DEFAULT;

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Random;

import org.anddev.andengine.engine.camera.Camera;
import org.anddev.andengine.engine.options.EngineOptions;
import org.anddev.andengine.engine.options.EngineOptions.ScreenOrientation;
import org.anddev.andengine.engine.options.resolutionpolicy.RatioResolutionPolicy;
import org.anddev.andengine.entity.primitive.Rectangle;
import org.anddev.andengine.entity.scene.Scene;
import org.anddev.andengine.entity.scene.Scene.IOnSceneTouchListener;
import org.anddev.andengine.entity.scene.background.ColorBackground;
import org.anddev.andengine.entity.shape.Shape;
import org.anddev.andengine.entity.sprite.TiledSprite;
import org.anddev.andengine.extension.physics.box2d.PhysicsConnector;
import org.anddev.andengine.extension.physics.box2d.PhysicsFactory;
import org.anddev.andengine.extension.physics.box2d.PhysicsWorld;
import org.anddev.andengine.extension.physics.box2d.util.Vector2Pool;
import org.anddev.andengine.extension.ui.livewallpaper.BaseLiveWallpaperService;
import org.anddev.andengine.input.touch.TouchEvent;
import org.anddev.andengine.opengl.texture.TextureOptions;
import org.anddev.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlas;
import org.anddev.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlasTextureRegionFactory;
import org.anddev.andengine.opengl.texture.region.TiledTextureRegion;
import org.anddev.andengine.sensor.accelerometer.AccelerometerData;
import org.anddev.andengine.sensor.accelerometer.IAccelerometerListener;

import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.hardware.SensorManager;
import android.util.DisplayMetrics;

import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.Manifold;

class BodyUserData
{
	// Fields
	TiledSprite	tiledSprite;

	// Constructor
	BodyUserData(final TiledSprite pTiledSprite)
	{
		tiledSprite = pTiledSprite;
	}
}

public class LiveWallpaperService extends BaseLiveWallpaperService implements IAccelerometerListener, IOnSceneTouchListener, SharedPreferences.OnSharedPreferenceChangeListener
{
	// ===========================================================
	// Constants
	// ===========================================================

	public static final String			SHARED_PREFS_NAME		= "livewallpapertemplatesettings";

	private static int					CAMERA_WIDTH;
	private static int					CAMERA_HEIGHT;
	private static final int			ANDROIDS				= 36;

	private static final FixtureDef		FIXTURE_DEF				= PhysicsFactory.createFixtureDef(1, 0.5f, 0.5f);
	private static final Random			random					= new Random();

	// ===========================================================
	// Fields
	// ===========================================================

	private Camera						mCamera;
	private BitmapTextureAtlas			mBitmapTextureAtlas;
	private final TiledTextureRegion	mAndroidTextureRegion[]	= new TiledTextureRegion[ANDROIDS];
	private PhysicsWorld				mPhysicsWorld;
	private static float				mForceImpuse;
	private RatioResolutionPolicy				mRatioResolutionPolicy;
	private ScreenOrientation			mScreenOrientation;
	private int							mSize;

	private ColorBackground				mColorBackground		= new ColorBackground(1.0f, 1.0f, 1.0f);

	//Shared Preferences
	private SharedPreferences			mSharedPreferences;
	private boolean						mSettingsChanged		= false;

	// ===========================================================
	// Constructors
	// ===========================================================

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	// ===========================================================
	// Methods for/from SuperClass/Interfaces
	// ===========================================================

	@Override
	public org.anddev.andengine.engine.Engine onLoadEngine()
	{
		Display display = ((WindowManager) getSystemService(WINDOW_SERVICE)).getDefaultDisplay();

		try
		{
			Method mGetRawH = Display.class.getMethod("getRawWidth");
			Method mGetRawW = Display.class.getMethod("getRawHeight");
			CAMERA_WIDTH = (Integer) mGetRawW.invoke(display);
			CAMERA_HEIGHT = (Integer) mGetRawH.invoke(display);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		int	rotation = display.getRotation();
		if (rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270)
		{
			mScreenOrientation = ScreenOrientation.LANDSCAPE;
			this.mCamera = new Camera(0, 0, CAMERA_WIDTH, CAMERA_HEIGHT);
			mRatioResolutionPolicy = new RatioResolutionPolicy(CAMERA_HEIGHT, CAMERA_WIDTH);
		}
		else
		{
			mScreenOrientation = ScreenOrientation.PORTRAIT;
			this.mCamera = new Camera(0, 0, CAMERA_WIDTH, CAMERA_HEIGHT);
			mRatioResolutionPolicy = new RatioResolutionPolicy(CAMERA_WIDTH, CAMERA_HEIGHT);
		}

		//mCamera = new Camera(0, 0, CAMERA_WIDTH, CAMERA_HEIGHT);
		return new org.anddev.andengine.engine.Engine(new EngineOptions(true, mScreenOrientation, mRatioResolutionPolicy, mCamera));

	}

	@Override
	public void onLoadResources()
	{
		mSharedPreferences = LiveWallpaperService.this.getSharedPreferences(SHARED_PREFS_NAME, 0);
		mSharedPreferences.registerOnSharedPreferenceChangeListener(this);
		onSharedPreferenceChanged(mSharedPreferences, null);

		if (Math.min(CAMERA_HEIGHT, CAMERA_WIDTH) > 720)
		{
			mSize = 128;
			mForceImpuse = -40;
			mBitmapTextureAtlas = new BitmapTextureAtlas(1024, 4 * mSize, TextureOptions.BILINEAR_PREMULTIPLYALPHA);
		}
		else
		{
			mSize = 64;
			mForceImpuse = -7;
			mBitmapTextureAtlas = new BitmapTextureAtlas(512, 4 * mSize, TextureOptions.BILINEAR_PREMULTIPLYALPHA);
		}
		BitmapTextureAtlasTextureRegionFactory.setAssetBasePath("gfx/" + mSize + "/");

		int width = 0;
		int i;

		for (i = 0; i < 9; i++)
		{
			mAndroidTextureRegion[i] = BitmapTextureAtlasTextureRegionFactory.createTiledFromAsset(mBitmapTextureAtlas, this, "android" + String.format("%02d", i) + ".png", width, 0, 1, 1);
			width += mAndroidTextureRegion[i].getWidth();
		}

		BitmapTextureAtlasTextureRegionFactory.createTiledFromAsset(mBitmapTextureAtlas, this, "blank.png", width, 0, 1, 1);

		width = 0;
		for (; i < 18; i++)
		{
			mAndroidTextureRegion[i] = BitmapTextureAtlasTextureRegionFactory.createTiledFromAsset(mBitmapTextureAtlas, this, "android" + String.format("%02d", i) + ".png", width, mSize, 1, 1);
			width += mAndroidTextureRegion[i].getWidth();
		}
		BitmapTextureAtlasTextureRegionFactory.createTiledFromAsset(mBitmapTextureAtlas, this, "blank.png", width, mSize, 1, 1);

		width = 0;
		for (; i < 27; i++)
		{
			mAndroidTextureRegion[i] = BitmapTextureAtlasTextureRegionFactory.createTiledFromAsset(mBitmapTextureAtlas, this, "android" + String.format("%02d", i) + ".png", width, 2 * mSize, 1, 1);
			width += mAndroidTextureRegion[i].getWidth();
		}
		BitmapTextureAtlasTextureRegionFactory.createTiledFromAsset(mBitmapTextureAtlas, this, "blank.png", width, 2 * mSize, 1, 1);

		width = 0;
		for (; i < ANDROIDS; i++)
		{
			mAndroidTextureRegion[i] = BitmapTextureAtlasTextureRegionFactory.createTiledFromAsset(mBitmapTextureAtlas, this, "android" + String.format("%02d", i) + ".png", width, 3 * mSize, 1, 1);
			width += mAndroidTextureRegion[i].getWidth();
		}
		BitmapTextureAtlasTextureRegionFactory.createTiledFromAsset(mBitmapTextureAtlas, this, "blank.png", width, 3 * mSize, 1, 1);
		getEngine().getTextureManager().loadTexture(mBitmapTextureAtlas);

	}

	public synchronized void  BuildScene(Scene scene)
	{
		// Destroy the current scene
		scene.detachChildren();

		// Create the scene with currentsettings
		scene.setBackground(mColorBackground);
		scene.setOnSceneTouchListener(this);
		enableAccelerometerSensor(this);
		scene.setTouchAreaBindingEnabled(true);

		mPhysicsWorld = new PhysicsWorld(new Vector2(0, SensorManager.GRAVITY_EARTH), false);

		mPhysicsWorld.setContactListener(new ContactListener()
		{
			@Override
			public void beginContact(final Contact pContact)
			{
				Body body = pContact.getFixtureA().getBody();
				BodyUserData userdata = (BodyUserData) body.getUserData();
				if (userdata != null)
				{
					userdata.tiledSprite.setCurrentTileIndex(0);
				}

				body = pContact.getFixtureB().getBody();
				userdata = (BodyUserData) body.getUserData();
				if (userdata != null)
				{
					userdata.tiledSprite.setCurrentTileIndex(0);
				}
			}

			@Override
			public void endContact(final Contact pContact)
			{
				Body body = pContact.getFixtureA().getBody();
				BodyUserData userdata = (BodyUserData) body.getUserData();
				if (userdata != null)
				{
					userdata.tiledSprite.setCurrentTileIndex(1);
				}

				body = pContact.getFixtureB().getBody();
				userdata = (BodyUserData) body.getUserData();
				if (userdata != null)
				{
					userdata.tiledSprite.setCurrentTileIndex(1);
				}
			}

			@Override
			public void postSolve(Contact arg0, ContactImpulse arg1)
			{
			}

			@Override
			public void preSolve(Contact arg0, Manifold arg1)
			{
			}
		});

		Shape ground;
		Shape roof;
		Shape left;
		Shape right;

		int rotation = ((WindowManager) getSystemService(WINDOW_SERVICE)).getDefaultDisplay().getRotation();

		if (rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270)
		{
			ground = new Rectangle(0, CAMERA_HEIGHT - 2, CAMERA_WIDTH, 2);
			roof = new Rectangle(0, 0, CAMERA_WIDTH, 2);
			left = new Rectangle(0, 0, 2, CAMERA_HEIGHT);
			right = new Rectangle(CAMERA_WIDTH - 2, 0, 2, CAMERA_HEIGHT);
		}
		else
		{
			ground = new Rectangle(0, CAMERA_WIDTH - 2, CAMERA_HEIGHT, 2);
			roof = new Rectangle(0, 0, 2, CAMERA_WIDTH);
			left = new Rectangle(0, 0, CAMERA_HEIGHT, 2);
			right = new Rectangle(CAMERA_HEIGHT, 0, 2, CAMERA_WIDTH - 2);
		}

		final FixtureDef wallFixtureDef = PhysicsFactory.createFixtureDef(0, 0.5f, 0.5f);
		PhysicsFactory.createBoxBody(mPhysicsWorld, ground, BodyType.StaticBody, wallFixtureDef);
		PhysicsFactory.createBoxBody(mPhysicsWorld, roof, BodyType.StaticBody, wallFixtureDef);
		PhysicsFactory.createBoxBody(mPhysicsWorld, left, BodyType.StaticBody, wallFixtureDef);
		PhysicsFactory.createBoxBody(mPhysicsWorld, right, BodyType.StaticBody, wallFixtureDef);

		scene.attachChild(ground);
		scene.attachChild(roof);
		scene.attachChild(left);
		scene.attachChild(right);

		scene.registerUpdateHandler(mPhysicsWorld);

		addFaces(scene);

	}

	@Override
	public Scene onLoadScene()
	{
		final Scene mScene = new Scene();
		BuildScene(mScene);
		return mScene;

	}

	@Override
	public void onLoadComplete()
	{

	}

	@Override
	public boolean onSceneTouchEvent(final Scene pScene, final TouchEvent pSceneTouchEvent)
	{
		if (mPhysicsWorld != null)
		{
			if (pSceneTouchEvent.isActionDown())
			{
				final Iterator<Body> bodies = mPhysicsWorld.getBodies();
				while (bodies.hasNext())
				{
					impulse(bodies.next(), pSceneTouchEvent.getX(), pSceneTouchEvent.getY());
				}
				return true;
			}
		}
		return false;
	}

	@Override
	public void onAccelerometerChanged(final AccelerometerData pAccelerometerData)
	{
		final Vector2 gravity = Vector2Pool.obtain(pAccelerometerData.getX(), pAccelerometerData.getY());
		mPhysicsWorld.setGravity(gravity);
		Vector2Pool.recycle(gravity);
	}

	private void addFaces(Scene scene)
	{

		for (int i = 0; i < ANDROIDS; i++)
		{
			final TiledSprite face = new TiledSprite((float) 50 + random.nextInt(400), (float) 50 + random.nextInt(400), mAndroidTextureRegion[i]);
			face.setCurrentTileIndex(1);
			final Body body = createHexagonBody(mPhysicsWorld, face, BodyType.DynamicBody, FIXTURE_DEF);
			body.setUserData(new BodyUserData(face));
			face.setUserData(body);

			scene.attachChild(face);
			mPhysicsWorld.registerPhysicsConnector(new PhysicsConnector(face, body));
		}

	}

	private static Body createHexagonBody(final PhysicsWorld pPhysicsWorld, final Shape pShape, final BodyType pBodyType, final FixtureDef pFixtureDef)
	{
		/* Remember that the vertices are relative to the center-coordinates of the Shape. */
		final float halfWidth = pShape.getWidthScaled() * 0.5f / PIXEL_TO_METER_RATIO_DEFAULT;
		final float halfHeight = pShape.getHeightScaled() * 0.5f / PIXEL_TO_METER_RATIO_DEFAULT;

		/* The top and bottom vertex of the hexagon are on the bottom and top of hexagon-sprite. */
		final float top = -halfHeight;
		final float bottom = halfHeight;

		final float centerX = 0;

		/* The left and right vertices of the heaxgon are not on the edge of the hexagon-sprite, so we need to inset them a little. */
		final float left = -halfWidth + 2.5f / PIXEL_TO_METER_RATIO_DEFAULT;
		final float right = halfWidth - 2.5f / PIXEL_TO_METER_RATIO_DEFAULT;
		final float higher = top + 8.25f / PIXEL_TO_METER_RATIO_DEFAULT;
		final float lower = bottom - 8.25f / PIXEL_TO_METER_RATIO_DEFAULT;

		final Vector2[] vertices = { new Vector2(centerX, top), new Vector2(right, higher), new Vector2(right, lower), new Vector2(centerX, bottom), new Vector2(left, lower), new Vector2(left, higher) };

		return PhysicsFactory.createPolygonBody(pPhysicsWorld, pShape, vertices, pBodyType, pFixtureDef);
	}

	private void impulse(final Body body, final float pX, final float pY)
	{
		final Vector2 velocity = Vector2Pool.obtain(new Vector2(body.getPosition().x - pX, body.getPosition().x - pY).nor().mul(mForceImpuse));
		body.applyAngularImpulse(mForceImpuse);
		Vector2Pool.recycle(velocity);
	}

	@Override
	public void onUnloadResources()
	{
	}

	@Override
	public void onPauseGame()
	{
		super.onPause();
	}

	@Override
	public void onResumeGame()
	{
		super.onResume();
		if (mSettingsChanged)
		{
			BuildScene(this.getEngine().getScene());
			mSettingsChanged = false;
		}
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig)
	{
		super.onConfigurationChanged(newConfig);

		BuildScene(this.getEngine().getScene());
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences pSharedPrefs, String pKey)
	{
		Integer color = 0;
		color = pSharedPrefs.getInt("colorPicker", 0xFFFFFFFF);

		mColorBackground.setColor(Color.red(color), Color.green(color), Color.blue(color));
		mSettingsChanged = true;

	}

	// ===========================================================
	// Methods
	// ===========================================================

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
}