package com.qualcomm.qti.polyqon;

import org.alljoyn.bus.BusAttachment;
import org.alljoyn.bus.BusException;
import org.alljoyn.bus.BusObject;
import org.alljoyn.bus.SignalEmitter;
import org.alljoyn.bus.Status;
import org.alljoyn.bus.annotation.BusSignalHandler;

import com.qualcomm.qti.polyqon.R;
import com.qualcomm.snapdragon.sdk.face.FaceData;
import com.qualcomm.snapdragon.sdk.face.FacialProcessing;
import com.qualcomm.snapdragon.sdk.face.FacialProcessing.PREVIEW_ROTATION_ANGLE;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Typeface;
import android.hardware.Camera;
import android.hardware.SensorManager;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;

import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class PolyqonActivity extends Activity implements Camera.PreviewCallback
{
	/* Load the native alljoyn_java library. */
	static
	{
		System.loadLibrary("alljoyn_java");
	}

	// Global Variables Required

	Camera							cameraObj;
	FrameLayout						preview;
	FacialProcessing				faceProc;
	FaceData[]						faceArray			= null;				// Array in which all the face data values will be returned for each face detected. 

	private CameraSurfacePreview	mPreview;
	private DrawView				drawView;
	private int						FRONT_CAMERA_INDEX	= 1;
	private int						BACK_CAMERA_INDEX	= 0;

	//boolean clicked = false;				
	boolean							_qcSDKEnabled		= false;
	boolean							cameraPause			= false;				// Boolean to check if the "pause" button is pressed or no. 
	static boolean					cameraSwitch		= false;				// Boolean to check if the camera is switched to back camera or no. 
	boolean							info				= false;				// Boolean to check if the face data info is displayed or no. 
	boolean							landScapeMode		= false;				// Boolean to check if the phone orientation is in landscape mode or portrait mode. 

	int								cameraIndex;								// Integer to keep track of which camera is open. 
	int								smileValue			= 0;
	int								leftEyeBlink		= 0;
	int								rightEyeBlink		= 0;
	int								faceRollValue		= 0;
	int								pitch				= 0;
	int								yaw					= 0;
	int								horizontalGaze		= 0;
	int								verticalGaze		= 0;
	PointF							gazePointValue		= null;

	//TextView Variables
	//TextView numFaceText, smileValueText, leftBlinkText, rightBlinkText, gazePointText, faceRollText, faceYawText, facePitchText, horizontalGazeText, verticalGazeText;	

	int								surfaceWidth		= 0;
	int								surfaceHeight		= 0;

	OrientationEventListener		orientationEventListener;
	int								deviceOrientation;
	int								presentOrientation;
	float							rounded;
	Display							display;
	int								displayAngle;

	private static final String		TAG					= "SessionlessChat";

	/* Handler for UI messages
	 * This handler updates the UI depending on the message received.
	 */
	private static final int		MESSAGE_CHAT		= 1;

	//private static final int		MESSAGE_POST_TOAST	= 2;

	MediaPlayer						mpYo;
	MediaPlayer						mpArr;

	private class PingInfo
	{
		private String	senderName;
		private String	message;

		public PingInfo(String senderName, String message)
		{
			this.senderName = senderName;
			this.message = message;
		}

		public String getSenderName()
		{
			return this.senderName;
		}

		public String getMessage()
		{
			return this.message;
		}
	}

	private Handler	mHandler		= new Handler(new Handler.Callback()
									{

										@Override
										public boolean handleMessage(Message msg)
										{
											switch (msg.what)
											{
												case MESSAGE_CHAT:
													/* Add the chat message received to the List View */
													String ping = (String) msg.obj;
													//Toast.makeText(getApplicationContext(), (String) ping,
													//		Toast.LENGTH_SHORT).show();
													if (ping.equalsIgnoreCase("arr"))
													{

														drawArrPhrase = true;
														arrPhraseCounter = 25;
														/*Gets your soundfile from intro.wav */
														//MediaPlayer mp = MediaPlayer.create(getBaseContext(), R.raw.sparta);
														mpArr.start();
														//														mp.setOnCompletionListener(new OnCompletionListener()
														//														{
														//
														//															@Override
														//															public void onCompletion(MediaPlayer mp)
														//															{
														//																mp.release();
														//															}
														//														});
													}
													else if (ping.equalsIgnoreCase("yo"))
													{
														drawYoPhrase = true;
														yoPhraseCounter = 12;

														/*Gets your soundfile from intro.wav */
														//MediaPlayer mp = MediaPlayer.create(getBaseContext(), R.raw.yo);
														mpYo.start();
														//														mp.setOnCompletionListener(new OnCompletionListener()
														//														{
														//
														//															@Override
														//															public void onCompletion(MediaPlayer mp)
														//															{
														//																mp.release();
														//															}
														//														});
													}

													//mListViewArrayAdapter.add(ping);
													break;
												//											case MESSAGE_POST_TOAST:
												//												/* Post a toast to the UI */
												//												Toast.makeText(getApplicationContext(), (String) msg.obj, Toast.LENGTH_LONG).show();
												//												break;
												default:
													break;
											}

											return true;
										}
									});

	/* UI elements */
	//private ListView mListView;
	// private EditText mNameEditText;
	// private EditText mMessageEditText;

	/* ArrayAdapter used to manage the chat messages
	 * received by this application.
	 */
	//private ArrayAdapter<String> mListViewArrayAdapter;

	/* Handler used to make calls to AllJoyn methods. See onCreate(). */
	private Handler	mBusHandler;

	public Paint	paint;

	public Bitmap	background;
	public Bitmap	sun;

	public int		sunY;
	public float	background_alpha;

	public int		lampX			= 800;
	public int		lampY			= 300;
	boolean			lamp_draw		= false;

	public int		pirateX			= 200;
	boolean			pirateDirection	= true;
	public int		rapperX			= 700;
	boolean			rapperDirection	= false;

	public int		characterY		= 1230;
	boolean			stepUp			= false;

	public Bitmap	cloud_left;
	public Bitmap	scaled_cloud;
	public Bitmap	lamp0;
	public Bitmap	lamp1;
	public Bitmap	tree_trunk;
	public Bitmap	tree;

	public Bitmap	pirate;
	public Bitmap	rapper;
	public Bitmap	grass;

	public Bitmap	yoPhase;
	public Bitmap	arrPhrase;

	public boolean	drawArrPhrase;
	public int		arrPhraseCounter;

	public boolean	drawYoPhrase;
	public int		yoPhraseCounter;

	public boolean	showPreview		= false;

	public float	cloudSize		= 2.0f;
	public float	cloudX			= -600;

	/**
	 * Launch Home activity helper
	 * 
	 * @param c context where launch home from (used by SplashscreenActivity)
	 */
	public static void launch(Context c)
	{
		Intent intent = new Intent(c, PolyqonActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		c.startActivity(intent);
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_camera_preview);

		// Create our Preview view and set it as the content of our activity.           
		preview = (FrameLayout) findViewById(R.id.camera_preview);

		// Check to see if the FacialProc feature is supported in the device or no. 
		_qcSDKEnabled = FacialProcessing.isFeatureSupported(FacialProcessing.FEATURE_LIST.FEATURE_FACIAL_PROCESSING);

		if (_qcSDKEnabled && faceProc == null)
		{
			Log.e("TAG", "Feature is supported");
			faceProc = FacialProcessing.getInstance(); // Calling the Facial Processing Constructor. 				
		}
		else
		{
			Log.e("TAG", "Feature is NOT supported");
			return;
		}

		cameraIndex = Camera.getNumberOfCameras() - 1; // Start with front Camera	

		try
		{
			cameraObj = Camera.open(cameraIndex); // attempt to get a Camera instance	   			        
		}
		catch (Exception e)
		{
			Log.d("TAG", "Camera Does Not exist"); // Camera is not available (in use or does not exist)
		}

		// Change the sizes according to phone's compatibility. 

		mPreview = new CameraSurfacePreview(PolyqonActivity.this, cameraObj, faceProc);
		preview.removeView(mPreview);
		preview = (FrameLayout) findViewById(R.id.camera_preview);
		preview.addView(mPreview);
		cameraObj.setPreviewCallback(PolyqonActivity.this);

		// Action listener for the screen touch to display the face data info. 
		//touchScreenListener();      

		vuforiaActionListener();

		// Action listener for the Switch Camera Button. 
		//cameraSwitchActionListener();
		showPreviewActionListener();

		arrActionListener();

		yoActionListener();

		orientationListener();

		display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE))
				.getDefaultDisplay();

		/* Make all AllJoyn calls through a separate handler thread to prevent blocking the UI. */
		HandlerThread busThread = new HandlerThread("BusHandler");
		busThread.start();
		mBusHandler = new Handler(busThread.getLooper(), new BusHandlerCallback());

		mBusHandler.sendEmptyMessage(BusHandlerCallback.CONNECT);

		paint = new Paint();
		paint.setAntiAlias(false);
		paint.setColor(Color.WHITE);
		
//    	SharedPreferences.Editor editor = getPreferences(MODE_PRIVATE).edit();
//   	 editor.putString("back", "background0");
//   	 editor.commit();
		
    	SharedPreferences prefs = getSharedPreferences("com.qualcomm.qti.polyqon", MODE_PRIVATE);
  	  SharedPreferences.Editor editor = prefs.edit();
  	  editor.putString("back", "background0");
  	  editor.commit(); 
  	  
//		SharedPreferences prefs = this.getSharedPreferences(
//  		      "com.qualcomm.qti.polyqon", Context.MODE_PRIVATE);
//  	prefs.edit().putString("back", "background0");

		background = BitmapFactory.decodeResource(getResources(),
				R.drawable.background);
		sun = BitmapFactory.decodeResource(getResources(),
				R.drawable.sun00);

		background_alpha = 1.0f;
		sunY = 300;
		cloud_left = BitmapFactory.decodeResource(getResources(),
				R.drawable.cloud_left);
		int newx = (int) ((float) cloud_left.getWidth() * cloudSize);
		int newy = (int) ((float) cloud_left.getHeight() * cloudSize);
		scaled_cloud = Bitmap.createScaledBitmap(cloud_left, newx, newy, true);

		lamp0 = BitmapFactory.decodeResource(getResources(),
				R.drawable.lamp0);
		lamp1 = BitmapFactory.decodeResource(getResources(),
				R.drawable.lamp1);
		tree_trunk = BitmapFactory.decodeResource(getResources(),
				R.drawable.tree_trunk);
		tree = BitmapFactory.decodeResource(getResources(),
				R.drawable.tree);
		pirate = BitmapFactory.decodeResource(getResources(),
				R.drawable.pirate_export);
		rapper = BitmapFactory.decodeResource(getResources(),
				R.drawable.rapper_export);
		grass = BitmapFactory.decodeResource(getResources(),
				R.drawable.grass);

		yoPhase = BitmapFactory.decodeResource(getResources(),
				R.drawable.yo_export);
		arrPhrase = BitmapFactory.decodeResource(getResources(),
				R.drawable.arr_export);
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
				PolyqonActivity.this);

		// set title
		alertDialogBuilder.setTitle("Your Title");

		// set dialog message
		alertDialogBuilder
				.setMessage("Click yes to exit!")
				.setCancelable(false)
				.setPositiveButton("Yes", new DialogInterface.OnClickListener()
				{
					public void onClick(DialogInterface dialog, int id)
					{
						// if this button is clicked, close
						// current activity
						//PolyqonActivity.this.finish();
					}
				})
				.setNegativeButton("No", new DialogInterface.OnClickListener()
				{
					public void onClick(DialogInterface dialog, int id)
					{
						// if this button is clicked, just close
						// the dialog box and do nothing
						dialog.cancel();
					}
				});

		// create alert dialog
		AlertDialog alertDialog = alertDialogBuilder.create();
		mpArr = MediaPlayer.create(getBaseContext(), R.raw.sparta);

		mpYo = MediaPlayer.create(getBaseContext(), R.raw.yo);
		// show it
		//alertDialog.show();

	}

	private void orientationListener()
	{
		orientationEventListener = new OrientationEventListener(this, SensorManager.SENSOR_DELAY_NORMAL)
		{
			@Override
			public void onOrientationChanged(int orientation)
			{
				deviceOrientation = orientation;
			}
		};

		if (orientationEventListener.canDetectOrientation())
		{
			orientationEventListener.enable();
		}

		presentOrientation = 90 * (deviceOrientation / 360) % 360;
	}

	/*
	 * Function for the screen touch action listener. On touching the screen, the face data info will be displayed. 
	 */
	/*private void touchScreenListener() {
		preview.setOnTouchListener(new OnTouchListener()
	    {

	        @Override
	        public boolean onTouch(View v, MotionEvent event)
	        {
	        	switch (event.getAction()) {
	            case MotionEvent.ACTION_DOWN:
	            	
	            	if(!info)
	            	{
	            		LayoutParams layoutParams = preview.getLayoutParams();	
	            		
	            		if(PolyqonActivity.this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
	            		{
	            			int oldHeight = preview.getHeight();
	            			layoutParams.height =oldHeight*3/4;
	            		}    
	            		else
	            		{
	            			int oldHeight = preview.getHeight();
	            			layoutParams.height =oldHeight*80/100;
	            		}
	              		preview.setLayoutParams(layoutParams);						// Setting the changed parameters for the layout. 
	              		info = true;                 		      		
	            	}
	            	else
	            	{            		
	            		LayoutParams layoutParams = preview.getLayoutParams();		
	              		layoutParams.height = LayoutParams.WRAP_CONTENT;
	              		preview.setLayoutParams(layoutParams);						// Setting the changed parameters for the layout. 
	              		info = false;  
	            	}
	              break;
	              
	            case MotionEvent.ACTION_MOVE:               	    
	              break;
	              
	            case MotionEvent.ACTION_UP:                 
	              break;
	            }
	        	
	        	return true;            	
	        }
	   });
		
	}*/

	/*
	 * Function for switch camera action listener. Switches camera from front to back and vice versa. 
	 */
	//	private void cameraSwitchActionListener()
	//	{
	//		ImageView switchButton = (ImageView) findViewById(R.id.switchCameraButton);
	//
	//		switchButton.setOnClickListener(new OnClickListener()
	//		{
	//
	//			@Override
	//			public void onClick(View arg0)
	//			{
	//
	//				if (!cameraSwitch) // If the camera is facing front then do this
	//				{
	//					stopCamera();
	//					cameraObj = Camera.open(BACK_CAMERA_INDEX);
	//					mPreview = new CameraSurfacePreview(PolyqonActivity.this, cameraObj, faceProc);
	//					preview = (FrameLayout) findViewById(R.id.camera_preview);
	//					preview.addView(mPreview);
	//					cameraSwitch = true;
	//					cameraObj.setPreviewCallback(PolyqonActivity.this);
	//				}
	//				else
	//				// If the camera is facing back then do this. 
	//				{
	//					stopCamera();
	//					cameraObj = Camera.open(FRONT_CAMERA_INDEX);
	//					preview.removeView(mPreview);
	//					mPreview = new CameraSurfacePreview(PolyqonActivity.this, cameraObj, faceProc);
	//					preview = (FrameLayout) findViewById(R.id.camera_preview);
	//					preview.addView(mPreview);
	//					cameraSwitch = false;
	//					cameraObj.setPreviewCallback(PolyqonActivity.this);
	//				}
	//
	//			}
	//
	//		});
	//	}

	private void showPreviewActionListener()
	{
		ImageView switchButton = (ImageView) findViewById(R.id.showPreviewButton);

		switchButton.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View arg0)
			{
				showPreview = !showPreview;
				Toast.makeText(getApplicationContext(), Boolean.toString(showPreview),
						Toast.LENGTH_SHORT).show();

			}

		});
	}

	private void arrActionListener()
	{
		ImageView switchButton = (ImageView) findViewById(R.id.arrButton);

		switchButton.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View arg0)
			{
				/* Send a sessionless signal chat message using the mBusHandler. */
				String senderName = "pirate";
				String message = "arr";

				Message msg = mBusHandler.obtainMessage(BusHandlerCallback.CHAT,
						new PingInfo(senderName, message));

				mBusHandler.sendMessage(msg);

			}

		});
	}

	private void yoActionListener()
	{
		ImageView switchButton = (ImageView) findViewById(R.id.yoButton);

		switchButton.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View arg0)
			{
				/* Send a sessionless signal chat message using the mBusHandler. */
				String senderName = "rapper";
				String message = "yo";

				Message msg = mBusHandler.obtainMessage(BusHandlerCallback.CHAT,
						new PingInfo(senderName, message));

				mBusHandler.sendMessage(msg);

			}

		});
	}

	//static final int	PICK_CONTACT_REQUEST	= 1334; // The request code

	/*
	 * Function for pause button action listener to pause and resume the preview. 
	 */
	private void vuforiaActionListener()
	{
		ImageView pause = (ImageView) findViewById(R.id.vuButton);
		pause.setOnClickListener(new OnClickListener()
		{

			@Override
			public void onClick(View arg0)
			{

				Intent intent = new Intent(PolyqonActivity.this, Books.class);
				//Intent i = new Intent();
				// i.setClassName(PolyqonActivity.this, Books.class);
				startActivity(intent);
				
				//startActivityForResult(intent, PICK_CONTACT_REQUEST);
				//				if (!cameraPause)
				//				{
				//					cameraObj.stopPreview();
				//					cameraPause = true;
				//				}
				//				else
				//				{
				//					cameraObj.startPreview();
				//					cameraObj.setPreviewCallback(PolyqonActivity.this);
				//					cameraPause = false;
				//				}

			}
		});
	}

//	@Override
//	protected void onActivityResult(int requestCode, int resultCode, Intent data)
//	{
//		Toast.makeText(getApplicationContext(), "result:",
//				Toast.LENGTH_SHORT).show();
//		 super.onActivityResult(requestCode, resultCode, data);
//		// Check which request we're responding to
//		if (requestCode == PICK_CONTACT_REQUEST)
//		{
//			// Make sure the request was successful
//			if (resultCode == RESULT_OK)
//			{
//				// The user picked a contact.
//				// The Intent's data Uri identifies which contact was selected.
//
//				Bundle res = data.getExtras();
//				String result = res.getString("param_result");
//				Log.d("FIRST", "result:" + result);
//				Toast.makeText(getApplicationContext(), "result:" + result,
//						Toast.LENGTH_SHORT).show();
//				background = BitmapFactory.decodeResource(getResources(),
//						R.drawable.background1);
//				// Do something with the contact here (bigger example below)
//			}
//		}
//	}

	/*
	 * This function will update the TextViews with the new values that come in. 
	 */

	//	public void setUI(int numFaces, int smileValue, int leftEyeBlink,
	//			int rightEyeBlink, int faceRollValue, int faceYawValue,
	//			int facePitchValue, PointF gazePointValue, int horizontalGazeAngle, int verticalGazeAngle)
	//	{
	//
	//		//		numFaceText.setText("Number of Faces: "+numFaces);
	//		//		smileValueText.setText("Smile Value: "+smileValue);		
	//		//		leftBlinkText.setText("Left Eye Blink Value: "+leftEyeBlink);
	//		//		rightBlinkText.setText("Right Eye Blink Value "+rightEyeBlink);
	//		//		faceRollText.setText("Face Roll Value: "+faceRollValue);
	//		//		faceYawText.setText("Face Yaw Value: "+faceYawValue);
	//		//		facePitchText.setText("Face Pitch Value: "+facePitchValue);
	//		//		horizontalGazeText.setText("Horizontal Gaze: "+horizontalGazeAngle);
	//		//		verticalGazeText.setText("VerticalGaze: "+verticalGazeAngle);
	//
	//		if (gazePointValue != null)
	//		{
	//			double x = Math.round(gazePointValue.x * 100.0) / 100.0; // Rounding the gaze point value. 
	//			double y = Math.round(gazePointValue.y * 100.0) / 100.0;
	//			//gazePointText.setText("Gaze Point: ("+x+","+y+")");
	//		}
	//		else
	//		{
	//			//gazePointText.setText("Gaze Point: ( , )");
	//		}
	//	}

	protected void onPause()
	{
		super.onPause();
		stopCamera();
	}

	protected void onDestroy()
	{
		super.onDestroy();
		background.recycle();
		background = null;
		sun.recycle();
		sun = null;
		grass.recycle();
		grass = null;

		cloud_left.recycle();
		cloud_left = null;
		lamp0.recycle();
		lamp0 = null;
		lamp1.recycle();
		lamp1 = null;
		tree_trunk.recycle();
		tree_trunk = null;
		tree.recycle();
		tree = null;

		pirate.recycle();
		pirate = null;
		rapper.recycle();
		rapper = null;

		yoPhase.recycle();
		yoPhase = null;
		arrPhrase.recycle();
		arrPhrase = null;

		mpArr.release();
		mpYo.release();

		/* Disconnect to prevent any resource leaks. */
		mBusHandler.sendEmptyMessage(BusHandlerCallback.DISCONNECT);

	}

	/* The class that is our AllJoyn service.  It implements the ChatInterface. */
	class ChatService implements ChatInterface, BusObject
	{
		public ChatService(BusAttachment bus)
		{
			this.bus = bus;
		}

		/*
		 * This is the Signal Handler code which has the interface name and the name of the signal
		 * which is sent by the client. It prints out the string it receives as parameter in the
		 * signal on the UI.
		 *
		 * This code also prints the string it received from the user and the string it is
		 * returning to the user to the screen.
		 */
		@BusSignalHandler(iface = "org.alljoyn.bus.samples.slchat", signal = "Chat")
		public void Chat(String senderName, String message)
		{
			Log.i(TAG, "Signal  : " + senderName + ": " + message);
			sendUiMessage(MESSAGE_CHAT, message);
		}

		/* Helper function to send a message to the UI thread. */
		private void sendUiMessage(int what, Object obj)
		{
			mHandler.sendMessage(mHandler.obtainMessage(what, obj));
		}

		private BusAttachment	bus;
	}

	/* This Callback class will handle all AllJoyn calls. See onCreate(). */
	class BusHandlerCallback implements Handler.Callback
	{

		/* The AllJoyn BusAttachment */
		private BusAttachment	mBus;

		/* The AllJoyn SignalEmitter used to emit sessionless signals */
		private SignalEmitter	emitter;

		private ChatInterface	mChatInterface	= null;
		private ChatService		myChatService	= null;

		/* These are the messages sent to the BusHandlerCallback from the UI. */
		public static final int	CONNECT			= 1;
		public static final int	DISCONNECT		= 2;
		public static final int	CHAT			= 3;

		@Override
		public boolean handleMessage(Message msg)
		{
			switch (msg.what)
			{
			/* Connect to the bus and register to obtain chat messages. */
				case CONNECT:
				{
					org.alljoyn.bus.alljoyn.DaemonInit.PrepareDaemon(getApplicationContext());
					/*
					 * All communication through AllJoyn begins with a BusAttachment.
					 *
					 * A BusAttachment needs a name. The actual name is unimportant except for internal
					 * security. As a default we use the class name as the name.
					 *
					 * By default AllJoyn does not allow communication between devices (i.e. bus to bus
					 * communication).  The second argument must be set to Receive to allow
					 * communication between devices.
					 */
					mBus = new BusAttachment(getPackageName(), BusAttachment.RemoteMessage.Receive);

					/*
					 * Create and register a bus object
					 */
					myChatService = new ChatService(mBus);
					Status status = mBus.registerBusObject(myChatService, "/ChatService");
					if (Status.OK != status)
					{
						logStatus("BusAttachment.registerBusObject()", status);
						return false;
					}

					/*
					 * Connect the BusAttachment to the bus.
					 */
					status = mBus.connect();
					logStatus("BusAttachment.connect()", status);
					if (status != Status.OK)
					{
						finish();
						return false;
					}

					/*
					 *  We register our signal handler which is implemented inside the ChatService
					 */
					status = mBus.registerSignalHandlers(myChatService);
					if (status != Status.OK)
					{
						Log.i(TAG, "Problem while registering signal handler");
						return false;
					}

					/*
					 *  Add rule to receive chat messages(sessionless signals)
					 */
					status = mBus.addMatch("sessionless='t'");
					if (status == Status.OK)
					{
						Log.i(TAG, "AddMatch was called successfully");
					}

					break;
				}

				/* Release all resources acquired in connect. */
				case DISCONNECT:
				{
					/*
					 * It is important to unregister the BusObject before disconnecting from the bus.
					 * Failing to do so could result in a resource leak.
					 */
					mBus.disconnect();
					mBusHandler.getLooper().quit();
					break;
				}
				/*
				 * Call the service's Ping method through the ProxyBusObject.
				 *
				 * This will also print the String that was sent to the service and the String that was
				 * received from the service to the user interface.
				 */
				case CHAT:
				{
					try
					{
						if (emitter == null)
						{
							/* Create an emitter to emit a sessionless signal with the desired message.
							 * The session ID is set to zero and the sessionless flag is set to true.
							 */
							emitter = new SignalEmitter(myChatService, 0, SignalEmitter.GlobalBroadcast.Off);
							emitter.setSessionlessFlag(true);
							/* Get the ChatInterface for the emitter */
							mChatInterface = emitter.getInterface(ChatInterface.class);
						}
						if (mChatInterface != null)
						{
							PingInfo info = (PingInfo) msg.obj;
							/* Send a sessionless signal using the chat interface we obtained. */
							Log.i(TAG, "Sending chat " + info.getSenderName() + ": " + info.getMessage());
							mChatInterface.Chat(info.getSenderName(), info.getMessage());
						}
					}
					catch (BusException ex)
					{
						logException("ChatInterface.Chat()", ex);
					}
					break;
				}
				default:
					break;
			}
			return true;
		}
	}

	private void logStatus(String msg, Status status)
	{
		String log = String.format("%s: %s", msg, status);
		if (status == Status.OK)
		{
			Log.i(TAG, log);
		}
		//		else
		//		{
		//			Message toastMsg = mHandler.obtainMessage(MESSAGE_POST_TOAST, log);
		//			mHandler.sendMessage(toastMsg);
		//			Log.e(TAG, log);
		//		}
	}

	private void logException(String msg, BusException ex)
	{
		String log = String.format("%s: %s", msg, ex);
		//		Message toastMsg = mHandler.obtainMessage(MESSAGE_POST_TOAST, log);
		//		mHandler.sendMessage(toastMsg);
		Log.e(TAG, log, ex);
	}

	@Override
	protected void onResume()
	{
		super.onResume();
		
//		SharedPreferences prefs = this.getSharedPreferences(
//  		      "com.qualcomm.qti.polyqon", Context.MODE_MULTI_PROCESS);
//
//		String restoredText = prefs.getString("back", null);
		
		SharedPreferences prefs = getSharedPreferences("com.qualcomm.qti.polyqon",
				  MODE_PRIVATE);
		String restoredText = prefs.getString("back",
				  null);
		
		if (restoredText != null) 
		{
			Toast.makeText(getApplicationContext(), "result:" + restoredText,
					Toast.LENGTH_SHORT).show();
			
			if(restoredText.equalsIgnoreCase("background1"))
			{
				background = BitmapFactory.decodeResource(getResources(),
						R.drawable.background1);
			}
			else if(restoredText.equalsIgnoreCase("background2"))
			{
				background = BitmapFactory.decodeResource(getResources(),
						R.drawable.background2);
			}

		}
		
		
		if (cameraObj != null)
		{
			stopCamera();
		}

		if (!cameraSwitch)
			startCamera(FRONT_CAMERA_INDEX);
		else
			startCamera(BACK_CAMERA_INDEX);
	}

	/*
	 * This is a function to stop the camera preview. Release the appropriate objects for later use. 
	 */
	public void stopCamera()
	{
		if (cameraObj != null)
		{
			cameraObj.stopPreview();
			cameraObj.setPreviewCallback(null);
			preview.removeView(mPreview);
			cameraObj.release();
			faceProc.release();
			faceProc = null;
		}

		cameraObj = null;
	}

	/*
	 * This is a function to start the camera preview. Call the appropriate constructors and objects. 
	 * @param-cameraIndex: Will specify which camera (front/back) to start. 
	 */
	public void startCamera(int cameraIndex)
	{

		if (_qcSDKEnabled && faceProc == null)
		{

			Log.e("TAG", "Feature is supported");
			faceProc = FacialProcessing.getInstance(); // Calling the Facial Processing Constructor. 				
		}

		try
		{
			cameraObj = Camera.open(cameraIndex); // attempt to get a Camera instance	   			        
		}
		catch (Exception e)
		{
			Log.d("TAG", "Camera Does Not exist"); // Camera is not available (in use or does not exist)
		}

		mPreview = new CameraSurfacePreview(PolyqonActivity.this, cameraObj, faceProc);
		preview.removeView(mPreview);
		preview = (FrameLayout) findViewById(R.id.camera_preview);
		preview.addView(mPreview);
		cameraObj.setPreviewCallback(PolyqonActivity.this);

	}

	//	@Override
	//	public boolean onCreateOptionsMenu(Menu menu) {
	//		// Inflate the menu; this adds items to the action bar if it is present.
	//		getMenuInflater().inflate(R.menu.camera_preview, menu);
	//		return true;
	//	}

	/*
	 * Detecting the face according to the new Snapdragon SDK. Face detection will now take place in this function. 
	 * 1) Set the Frame
	 * 2) Detect the Number of faces. 
	 * 3) If(numFaces > 0) then do the necessary processing. 
	 */
	@Override
	public void onPreviewFrame(byte[] data, Camera arg1)
	{

		presentOrientation = (90 * Math.round(deviceOrientation / 90)) % 360;
		int dRotation = display.getRotation();
		PREVIEW_ROTATION_ANGLE angleEnum = PREVIEW_ROTATION_ANGLE.ROT_0;

		switch (dRotation)
		{
			case 0:
				displayAngle = 90;
				angleEnum = PREVIEW_ROTATION_ANGLE.ROT_90;
				break;

			case 1:
				displayAngle = 0;
				angleEnum = PREVIEW_ROTATION_ANGLE.ROT_0;
				break;

			case 2:
				// This case is never reached. 
				break;

			case 3:
				displayAngle = 180;
				angleEnum = PREVIEW_ROTATION_ANGLE.ROT_180;
				break;
		}

		if (faceProc == null)
		{
			faceProc = FacialProcessing.getInstance();
		}

		Parameters params = cameraObj.getParameters();
		Size previewSize = params.getPreviewSize();
		surfaceWidth = mPreview.getWidth();
		surfaceHeight = mPreview.getHeight();

		// Landscape mode - front camera 
		if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE && !cameraSwitch)
		{
			faceProc.setFrame(data, previewSize.width, previewSize.height, true, angleEnum);
			cameraObj.setDisplayOrientation(displayAngle);
			landScapeMode = true;
		}
		// landscape mode - back camera
		else if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE && cameraSwitch)
		{
			faceProc.setFrame(data, previewSize.width, previewSize.height, false, angleEnum);
			cameraObj.setDisplayOrientation(displayAngle);
			landScapeMode = true;
		}
		// Portrait mode - front camera
		else if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT && !cameraSwitch)
		{
			faceProc.setFrame(data, previewSize.width, previewSize.height, true, angleEnum);
			cameraObj.setDisplayOrientation(displayAngle);
			landScapeMode = false;
		}
		// Portrait mode - back camera
		else
		{
			faceProc.setFrame(data, previewSize.width, previewSize.height, false, angleEnum);
			cameraObj.setDisplayOrientation(displayAngle);
			landScapeMode = false;
		}

		cloudX += 30;
		if (cloudX > 1080)
		{
			cloudX = -600;
		}

		if (pirateDirection)
		{
			pirateX += 20;
		}
		else
		{
			pirateX -= 20;
		}

		if (pirateX >= 930)
		{
			pirateDirection = false;
		}
		else if (pirateX <= 0)
		{
			pirateDirection = true;
		}

		if (rapperDirection)
		{
			rapperX += 30;
		}
		else
		{
			rapperX -= 30;
		}

		if (arrPhraseCounter > 0)
		{
			arrPhraseCounter--;
			if (arrPhraseCounter == 0)
			{
				drawArrPhrase = false;
			}
		}

		if (yoPhraseCounter > 0)
		{
			yoPhraseCounter--;
			if (yoPhraseCounter == 0)
			{
				drawYoPhrase = false;
			}
		}

		if (rapperX >= 930)
		{
			rapperDirection = false;
		}
		else if (rapperX <= 0)
		{
			rapperDirection = true;
		}

		if (stepUp)
		{
			characterY -= 15;

		}
		else
		{
			characterY += 15;
		}

		if (characterY >= 1260)
		{
			stepUp = true;
		}
		else if (characterY <= 1200)
		{
			stepUp = false;
		}

		int numFaces = faceProc.getNumFaces();

		System.gc();
		if (numFaces == 0)
		{
			Log.d("TAG", "No Face Detected");
			if (drawView != null)
			{
				preview.removeView(drawView);
			}

			drawView = new DrawView(this, null,
					false, 0, 0, null,
					landScapeMode);
			preview.addView(drawView);

			//setUI(0, 0, 0, 0, 0, 0, 0, null, 0, 0);
		}
		else
		{

			Log.d("TAG", "Face Detected");
			faceArray = faceProc.getFaceData();

			if (faceArray == null)
			{
				Log.e("TAG", "Face array is null");
			}
			else
			{
				faceProc.normalizeCoordinates(surfaceWidth, surfaceHeight);
				if (drawView != null)
				{
					preview.removeView(drawView); // Remove the previously created view to avoid unnecessary stacking of Views.
				}
				drawView = new DrawView(this, faceArray, true,
						surfaceWidth, surfaceHeight, cameraObj,
						landScapeMode);
				preview.addView(drawView);

				smileValue = faceArray[0].getSmileValue();
				if (smileValue > 70)
				{
					sunY -= 40;
				}
				else if (smileValue < 20)
				{
					sunY += 20;
				}
				if (sunY >= 1920)
				{
					sunY = 1920;
				}
				if (sunY <= 10)
				{
					sunY = 0;
				}

				leftEyeBlink = faceArray[0].getLeftEyeBlink();
				if (leftEyeBlink < 50)
				{
					cloudSize -= 0.05;
				}
				else if (leftEyeBlink > 50)
				{
					cloudSize += 0.05f;
				}

				if (cloudSize < 0.5f)
					cloudSize = 0.5f;

				if (cloudSize > 1.9f)
					cloudSize = 1.9f;

				//				if(scaled_cloud != null)
				//				{
				//					//scaled_cloud.recycle();
				//					//scaled_cloud = null;
				//				}

				int newx = (int) ((float) cloud_left.getWidth() * cloudSize);
				int newy = (int) ((float) cloud_left.getHeight() * cloudSize);
				scaled_cloud = Bitmap.createScaledBitmap(cloud_left, newx, newy, true);

				rightEyeBlink = faceArray[0].getRightEyeBlink();
				faceRollValue = faceArray[0].getRoll();

				gazePointValue = faceArray[0].getEyeGazePoint();
				pitch = faceArray[0].getPitch();
				if (pitch > 0)
				{
					lampY += 40;
				}
				else if (pitch < 0)
				{
					lampY -= 40;
				}
				if (lampY <= 0)
				{
					lampY = 0;
				}
				else if (lampY >= 930)
				{
					lampY = 930;
				}
				yaw = faceArray[0].getYaw();

				if (yaw < 0)
				{
					lampX += 40;
				}
				else if (yaw > 0)
				{
					lampX -= 40;
				}
				if (lampX <= 0)
				{
					lampX = 0;
				}
				else if (lampX >= 930)
				{
					lampX = 930;
				}
				horizontalGaze = faceArray[0].getEyeHorizontalGazeAngle();
				verticalGaze = faceArray[0].getEyeVerticalGazeAngle();

				//setUI(numFaces, smileValue, leftEyeBlink, rightEyeBlink, faceRollValue, yaw, pitch, gazePointValue, horizontalGaze, verticalGaze);
			}

		}
	}

}