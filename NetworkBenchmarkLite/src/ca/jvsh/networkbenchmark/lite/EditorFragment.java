package ca.jvsh.networkbenchmark.lite;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xmlpull.v1.XmlSerializer;

import ca.jvsh.networkbenchmark.lite.R;
import ca.jvsh.networkbenchmark.lite.TestingThread.TestingSequence;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.lamerman.FileDialog;
import com.lamerman.SelectionMode;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.util.SparseArray;
import android.util.TypedValue;
import android.util.Xml;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

public class EditorFragment extends SherlockFragment
{

	private EditText					mConfigurationFilePathEdit;
	private LinearLayout				mConfigurationLinearLayout;
	private Button						mAddNewThreadButton;

	View								mView;
	private Context						mContext;

	private static final LayoutParams	mBasicLinearLayoutParams	= new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
	private static final LayoutParams	mWeightLayoutParams			= new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 1.0f);
	private static final InputFilter[]	mFilterArray				= new InputFilter[1];
	// Debugging tag.
	private static final String			TAG							= "EditorFragment";
	protected static final int			MSG_SERVER_SOCKET_ERR		= 0;
	protected static final int			MSG_BYTES_RECEIVED			= 1;

	protected static final int			MAGIC_THREAD_ID				= 1000;
	protected static final int			MAGIC_SEQ_ID				= 20;

	private static final int			THREAD_ADD_SEQ_BUTTON_ID	= 5;
	private static final int			THREAD_DELETE_BUTTON_ID		= 4;
	private static final int			THREAD_HEADER_TEXT_ID		= 3;
	private static final int			THREAD_HEADER_LAYOUT_ID		= 2;
	private static final int			THREAD_LAYOUT_ID			= 1;
	//this is where we will start
	public SparseArray<TestingThread>	mTestingThreads;

	//for special purposes - to pass it inside the alertbox dialog
	int									mDeleteThreadId;
	int									mDeleteSequenceId;

	boolean								mConfigurationChanged		= false;
	MenuItem							mSaveMenuItem;

	/**
	 * Create a new instance of CountingFragment, providing "num"
	 * as an argument.
	 */
	static EditorFragment newInstance(int num)
	{
		return new EditorFragment();
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);

		mTestingThreads = new SparseArray<TestingThread>();
	}

	/**
	 * The Fragment's UI is just a simple text view showing its
	 * instance number.
	 */
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		mView = inflater.inflate(R.layout.fragment_editor, container, false);
		mContext = mView.getContext();

		//set basic linear layout parameters

		int tenDpInPx = getPixels(10);
		mBasicLinearLayoutParams.setMargins(getPixels(20), tenDpInPx, tenDpInPx, tenDpInPx);
		mWeightLayoutParams.gravity = Gravity.CENTER_VERTICAL;

		mConfigurationFilePathEdit = (EditText) mView.findViewById(R.id.editTextEditorConfigurationFilePath);
		mConfigurationFilePathEdit.addTextChangedListener(mEditConfigFileChangeTextWatcher);

		mConfigurationLinearLayout = (LinearLayout) mView.findViewById(R.id.linearLayoutThreads);

		mFilterArray[0] = new InputFilter.LengthFilter(6);

		mAddNewThreadButton = (Button) mView.findViewById(R.id.buttonAddNewThread);
		mAddNewThreadButton.setFocusableInTouchMode(true);
		mAddNewThreadButton.requestFocus();
		mAddNewThreadButton.setOnClickListener(new OnClickListener()
		{
			public void onClick(View v)
			{
				int threadsArraySize = mTestingThreads.size();

				if (threadsArraySize >= 10)
				{
					AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mContext);

					// set title
					alertDialogBuilder.setTitle("Too many threads");

					// set dialog message
					alertDialogBuilder.setMessage("Configuration can have 10 threads maximum");
					alertDialogBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener()
					{
						public void onClick(DialogInterface dialog, int id)
						{
						}
					});

					// create alert dialog
					AlertDialog alertDialog = alertDialogBuilder.create();

					// show it
					alertDialog.show();

					return;
				}

				int threadId = 0;

				if (threadsArraySize != 0)
				{
					threadId = mTestingThreads.keyAt(threadsArraySize - 1) + 1;
				}

				addThread(threadId);
			}
		});
		return mView;
	}

	@Override
	public void onResume()
	{
		super.onResume();

	}

	@Override
	public void onStop()
	{
		//////////////////////////////////
		//saving parameters
		//////////////////////////////////
		Editor editor = PreferenceManager.getDefaultSharedPreferences(mContext).edit();

		editor.commit();

		//stop client threads
		super.onStop();
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
	{
		inflater.inflate(R.menu.fragment_editor_menu, menu);
		mSaveMenuItem = menu.getItem(2);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		// Handle item selection
		switch (item.getItemId())
		{
			case R.id.editor_new:

				onMenuItemOpen(true);

				return true;

			case R.id.editor_open:

				onMenuItemOpen(false);
				return true;

			case R.id.editor_save:
				
				mAddNewThreadButton.requestFocus();
				
				saveXmlConfiguration();
				return true;

			case R.id.editor_help:
				return true;

			default:
				return super.onOptionsItemSelected(item);
		}
	}

	public void onMenuItemOpen(boolean new_config)
	{

		if (mConfigurationChanged)
		{
			AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mContext);

			// set title
			alertDialogBuilder.setTitle("Configuration change");

			// set dialog message
			alertDialogBuilder.setMessage("Configuration was changed.\nSave it?");

			if (new_config)
			{
				alertDialogBuilder.setPositiveButton("Yes", new DialogInterface.OnClickListener()
				{
					public void onClick(DialogInterface dialog, int id)
					{
						saveXmlConfiguration();
						onOpenFileDialog(true);
					}
				});
				alertDialogBuilder.setNeutralButton("No", new DialogInterface.OnClickListener()
				{
					public void onClick(DialogInterface dialog, int id)
					{
						onOpenFileDialog(true);
					}
				});
			}
			else
			{
				alertDialogBuilder.setPositiveButton("Yes", new DialogInterface.OnClickListener()
				{
					public void onClick(DialogInterface dialog, int id)
					{
						saveXmlConfiguration();
						onOpenFileDialog(false);
					}
				});
				alertDialogBuilder.setNeutralButton("No", new DialogInterface.OnClickListener()
				{
					public void onClick(DialogInterface dialog, int id)
					{
						onOpenFileDialog(false);
					}
				});
			}

			alertDialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener()
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

			// show it
			alertDialog.show();
		}
		else
		{
			onOpenFileDialog(new_config);
		}
	}

	public void onOpenFileDialog(boolean new_config)
	{
		clearEditor();

		Intent intentOpen = new Intent(mContext, FileDialog.class);
		intentOpen.putExtra(FileDialog.START_PATH, Environment.getExternalStorageDirectory().getPath());

		//can user select directories or not
		intentOpen.putExtra(FileDialog.CAN_SELECT_DIR, false);

		//alternatively you can set file filter
		intentOpen.putExtra(FileDialog.FORMAT_FILTER, new String[] { "xml" });

		if (new_config)
		{
			intentOpen.putExtra(FileDialog.SELECTION_MODE, SelectionMode.MODE_CREATE);
			startActivityForResult(intentOpen, SelectionMode.MODE_CREATE);
		}
		else
		{
			intentOpen.putExtra(FileDialog.SELECTION_MODE, SelectionMode.MODE_OPEN);
			startActivityForResult(intentOpen, SelectionMode.MODE_OPEN);
		}
	}

	public synchronized void onActivityResult(final int requestCode,
			int resultCode, final Intent data)
	{

		if (resultCode == Activity.RESULT_OK)
		{
			String filePath = data.getStringExtra(FileDialog.RESULT_PATH);

			mConfigurationFilePathEdit.setText(filePath);
			mAddNewThreadButton.setEnabled(true);

			if (requestCode == SelectionMode.MODE_CREATE)
			{
				Log.d(TAG, "Creating new configuration...");

				//if we just created a configuration - load it immidiately
				mConfigurationChanged = true;
				Log.d(TAG, "onActivityResult mSaveMenuItem.setVisible(true);");
				mSaveMenuItem.setVisible(true);

				addThread(0);
				//add at least one new sequence to the thread
				addSequence(0, 0, 10000, 1000, 100, -1);

			}
			else if (requestCode == SelectionMode.MODE_OPEN)
			{
				Log.d(TAG, "Opening old configuration...");

				loadXmlConfiguration(filePath);
				mConfigurationChanged = false;
				Log.d(TAG, "onActivityResult mSaveMenuItem.setVisible(false);");
				mSaveMenuItem.setVisible(false);
			}

		}
		else if (resultCode == Activity.RESULT_CANCELED)
		{
			mConfigurationFilePathEdit.setText("");
			mSaveMenuItem.setVisible(false);
			mAddNewThreadButton.setEnabled(false);
		}

	}

	private void addThread(int threadId)
	{
		Log.d(TAG, "Thread id " + threadId);
		mTestingThreads.put(threadId, new TestingThread(0, threadId));

		int basicControlsId = (threadId + 1) * MAGIC_THREAD_ID;

		//this is the bottom, most basic layout
		LinearLayout threadLinearLayout = new LinearLayout(mContext);
		threadLinearLayout.setId(basicControlsId + THREAD_LAYOUT_ID);
		{
			threadLinearLayout.setOrientation(LinearLayout.VERTICAL);
			threadLinearLayout.setBackgroundResource(R.drawable.border);

			threadLinearLayout.setLayoutParams(mBasicLinearLayoutParams);
		}

		LinearLayout threadHeaderLinearLayout = new LinearLayout(mContext);
		threadHeaderLinearLayout.setId(basicControlsId + THREAD_HEADER_LAYOUT_ID);

		TextView threadIdTextView = new TextView(mContext);
		threadHeaderLinearLayout.setId(basicControlsId + THREAD_HEADER_TEXT_ID);

		ImageButton deletethreadButton = new ImageButton(mContext);
		deletethreadButton.setId(basicControlsId + THREAD_DELETE_BUTTON_ID);
		deletethreadButton.setOnClickListener(deleteThreadButtonListener);
		//set button and thread id text view
		{
			threadIdTextView.setText("Thread " + (threadId + 1));
			threadIdTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
			threadIdTextView.setLayoutParams(mWeightLayoutParams);

			deletethreadButton.setImageResource(R.drawable.delete);

			threadHeaderLinearLayout.addView(threadIdTextView);
			threadHeaderLinearLayout.addView(deletethreadButton);

			threadLinearLayout.addView(threadHeaderLinearLayout);
		}

		//////////////////////////////////////////////////////
		//button to add additional sequence 
		////////////////////////////////////////////////////
		Button addSequenceButton = new Button(mContext);
		addSequenceButton.setText("Add sequence");
		addSequenceButton.setOnClickListener(addSequenceButtonListener);
		addSequenceButton.setId(basicControlsId + THREAD_ADD_SEQ_BUTTON_ID);

		threadLinearLayout.addView(addSequenceButton);

		mConfigurationLinearLayout.addView(threadLinearLayout);

		mConfigurationChanged = true;
		mSaveMenuItem.setVisible(true);

	}

	OnClickListener	deleteThreadButtonListener	= new OnClickListener()
												{
													public void onClick(View v)
													{

														int buttonId = v.getId();

														mDeleteThreadId = (buttonId / MAGIC_THREAD_ID) - 1;

														AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mContext);

														// set title
														alertDialogBuilder.setTitle("Thread deletion");

														// set dialog message
														alertDialogBuilder.setMessage("Delete Thread " + (mDeleteThreadId + 1) + "?");
														//alertDialogBuilder.setCancelable(false)
														alertDialogBuilder.setPositiveButton("Yes", new DialogInterface.OnClickListener()
														{
															public void onClick(DialogInterface dialog, int id)
															{
																removeThreadAt(mDeleteThreadId);
															}
														});
														alertDialogBuilder.setNegativeButton("No", new DialogInterface.OnClickListener()
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

														// show it
														alertDialog.show();

													}
												};

	private void removeThreadAt(int threadId)
	{
		int basicControlsId = (threadId + 1) * MAGIC_THREAD_ID;

		Log.d(TAG, "Deleting thread with id " + threadId);

		TestingThread testingThread = mTestingThreads.get(threadId);

		//we need to delete all sequences first
		while (testingThread.mTestingSequences.size() != 0)
		{
			int key = testingThread.mTestingSequences.keyAt(0);
			removeSequenceAt(threadId, key);
		}

		mConfigurationLinearLayout.removeView(mConfigurationLinearLayout.findViewById(basicControlsId + THREAD_ADD_SEQ_BUTTON_ID));
		mConfigurationLinearLayout.removeView(mConfigurationLinearLayout.findViewById(basicControlsId + THREAD_DELETE_BUTTON_ID));
		mConfigurationLinearLayout.removeView(mConfigurationLinearLayout.findViewById(basicControlsId + THREAD_HEADER_TEXT_ID));
		mConfigurationLinearLayout.removeView(mConfigurationLinearLayout.findViewById(basicControlsId + THREAD_HEADER_LAYOUT_ID));
		mConfigurationLinearLayout.removeView(mConfigurationLinearLayout.findViewById(basicControlsId + THREAD_LAYOUT_ID));

		mTestingThreads.delete(threadId);

		mConfigurationChanged = true;
		mSaveMenuItem.setVisible(true);
	}

	OnClickListener	addSequenceButtonListener	= new OnClickListener()
												{
													public void onClick(View v)
													{
														int buttonId = v.getId();

														int threadId = (buttonId / MAGIC_THREAD_ID) - 1;

														TestingThread testingThread = mTestingThreads.get(threadId);

														int seqSize = testingThread.mTestingSequences.size();
														Log.d(TAG, "seqSize " + seqSize);

														if (seqSize >= 40)
														{
															AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mContext);

															// set title
															alertDialogBuilder.setTitle("Too many sequences");

															// set dialog message
															alertDialogBuilder.setMessage("Thread can have 40 sequences maximum");
															alertDialogBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener()
															{
																public void onClick(DialogInterface dialog, int id)
																{
																}
															});

															// create alert dialog
															AlertDialog alertDialog = alertDialogBuilder.create();

															// show it
															alertDialog.show();

															return;
														}

														int seqId = 0;

														if (seqSize != 0)
														{
															seqId = testingThread.mTestingSequences.keyAt(seqSize - 1) + 1;
														}

														addSequence(threadId, seqId, 10000, 1000, 100, -1);
													}
												};

	private void addSequence(int threadId, int seqId, int time_total, int bytes_send, long delay_nano, int repeat)
	{
		int basicControlsId = (threadId + 1) * MAGIC_THREAD_ID;

		Log.d(TAG, "Thread id " + threadId + " Seq id " + seqId);

		TestingSequence testingSequence = mTestingThreads.get(threadId).new TestingSequence();
		{
			testingSequence.time_total = time_total;
			testingSequence.bytes_send = bytes_send;
			testingSequence.delay_nano = delay_nano;
			testingSequence.repeat = repeat;
		}

		mTestingThreads.get(threadId).mTestingSequences.put(seqId, testingSequence);

		int sequenceControlsId = basicControlsId + (seqId + 1) * MAGIC_SEQ_ID;

		LinearLayout threadLinearLayout = (LinearLayout) mConfigurationLinearLayout.findViewById(basicControlsId + THREAD_LAYOUT_ID);

		LinearLayout sequenceLinearLayout = new LinearLayout(mContext);
		sequenceLinearLayout.setId(sequenceControlsId + 1);
		{
			sequenceLinearLayout.setOrientation(LinearLayout.VERTICAL);
			sequenceLinearLayout.setLayoutParams(mBasicLinearLayoutParams);
		}

		LinearLayout sequenceHeaderLinearLayout = new LinearLayout(mContext);
		sequenceHeaderLinearLayout.setId(sequenceControlsId + 2);

		TextView sequenceIdTextView = new TextView(mContext);
		sequenceIdTextView.setId(sequenceControlsId + 3);
		ImageButton deleteSequenceButton = new ImageButton(mContext);
		deleteSequenceButton.setId(sequenceControlsId + 4);
		deleteSequenceButton.setOnClickListener(deleteSequenceButtonListener);

		{
			sequenceIdTextView.setText("Sequence " + (seqId + 1));
			sequenceIdTextView.setLayoutParams(mWeightLayoutParams);

			deleteSequenceButton.setImageResource(R.drawable.delete);

			sequenceHeaderLinearLayout.addView(sequenceIdTextView);
			sequenceHeaderLinearLayout.addView(deleteSequenceButton);

			sequenceLinearLayout.addView(sequenceHeaderLinearLayout);
		}

		TableLayout sequenceParametersTableLayout = new TableLayout(mContext);
		sequenceParametersTableLayout.setId(sequenceControlsId + 5);
		sequenceParametersTableLayout.setLayoutParams(mBasicLinearLayoutParams);

		///////////////////////////////////////////////////////////////////////
		//time total layout
		//////////////////////////////////////////////////////////////////////

		TableRow timeTotalRow = new TableRow(mContext);
		timeTotalRow.setId(sequenceControlsId + 6);

		TextView timeTotalTextView = new TextView(mContext);
		timeTotalTextView.setId(sequenceControlsId + 7);
		EditText timeTotalEdit = new EditText(mContext);
		timeTotalEdit.setId(sequenceControlsId + 8);

		{
			timeTotalTextView.setText("Total time (ms)");
			timeTotalEdit.setEms(10);
			timeTotalEdit.setSingleLine(true);
			timeTotalEdit.setInputType(InputType.TYPE_CLASS_NUMBER);
			timeTotalEdit.setFilters(mFilterArray);
			timeTotalEdit.setText(Integer.toString(time_total));
			timeTotalEdit.addTextChangedListener(mEditSequenceChangeTextWatcher);
			timeTotalEdit.setOnFocusChangeListener(new OnFocusChangeListener()
			{

				public void onFocusChange(View v, boolean hasFocus)
				{
					if (!hasFocus)
					{
						EditText edit = (EditText) v;
						int editId = edit.getId();

						int threadId = (editId / MAGIC_THREAD_ID) - 1;
						int basicControlsId = (threadId + 1) * MAGIC_THREAD_ID;

						int seqId = (editId - basicControlsId) / MAGIC_SEQ_ID - 1;

						String text = edit.getText().toString();
						Log.d(TAG, "Setting time_total param using (" + text + ") for the sequence " + seqId + " of the thread " + threadId);
						mTestingThreads.get(threadId).mTestingSequences.get(seqId).time_total = text.isEmpty() ? 0 : Integer.parseInt(text);
					}
				}
			});

			timeTotalRow.addView(timeTotalTextView);
			timeTotalRow.addView(timeTotalEdit);

			sequenceParametersTableLayout.addView(timeTotalRow);
		}

		///////////////////////////////////////////////////////////////////////
		//delay milliseconds layout
		//////////////////////////////////////////////////////////////////////

		TableRow delayNsRow = new TableRow(mContext);
		delayNsRow.setId(sequenceControlsId + 9);

		TextView delayNsTextView = new TextView(mContext);
		delayNsTextView.setId(sequenceControlsId + 10);
		EditText delayNsEdit = new EditText(mContext);
		delayNsEdit.setId(sequenceControlsId + 11);

		{
			delayNsTextView.setText("Delay (ns)");
			delayNsEdit.setEms(10);
			delayNsEdit.setSingleLine(true);
			delayNsEdit.setInputType(InputType.TYPE_CLASS_NUMBER);
			delayNsEdit.setFilters(mFilterArray);
			delayNsEdit.setText(Long.toString(delay_nano));
			delayNsEdit.addTextChangedListener(mEditSequenceChangeTextWatcher);
			delayNsEdit.setOnFocusChangeListener(new OnFocusChangeListener()
			{

				public void onFocusChange(View v, boolean hasFocus)
				{
					if (!hasFocus)
					{
						EditText edit = (EditText) v;
						int editId = edit.getId();

						int threadId = (editId / MAGIC_THREAD_ID) - 1;
						int basicControlsId = (threadId + 1) * MAGIC_THREAD_ID;

						int seqId = (editId - basicControlsId) / MAGIC_SEQ_ID - 1;

						String text = edit.getText().toString();
						Log.d(TAG, "Setting delay_ns param using (" + text + ") for the sequence " + seqId + " of the thread " + threadId);
						mTestingThreads.get(threadId).mTestingSequences.get(seqId).delay_nano =text.isEmpty() ? 0L : Long.parseLong(text) ;
					}
				}
			});

			delayNsRow.addView(delayNsTextView);
			delayNsRow.addView(delayNsEdit);

			sequenceParametersTableLayout.addView(delayNsRow);
		}

		///////////////////////////////////////////////////////////////////////
		//bytes layout
		//////////////////////////////////////////////////////////////////////
		TableRow bytesRow = new TableRow(mContext);
		bytesRow.setId(sequenceControlsId + 12);

		TextView bytesTextView = new TextView(mContext);
		bytesTextView.setId(sequenceControlsId + 13);
		EditText bytesEdit = new EditText(mContext);
		bytesEdit.setId(sequenceControlsId + 14);

		{
			bytesTextView.setText("Bytes");
			bytesEdit.setEms(10);
			bytesEdit.setSingleLine(true);
			bytesEdit.setInputType(InputType.TYPE_CLASS_NUMBER);
			bytesEdit.setFilters(mFilterArray);
			bytesEdit.setText(Integer.toString(bytes_send));
			bytesEdit.addTextChangedListener(mEditSequenceChangeTextWatcher);
			bytesEdit.setOnFocusChangeListener(new OnFocusChangeListener()
			{

				public void onFocusChange(View v, boolean hasFocus)
				{
					if (!hasFocus)
					{
						EditText edit = (EditText) v;
						int editId = edit.getId();

						int threadId = (editId / MAGIC_THREAD_ID) - 1;
						int basicControlsId = (threadId + 1) * MAGIC_THREAD_ID;

						int seqId = (editId - basicControlsId) / MAGIC_SEQ_ID - 1;

						String text = edit.getText().toString();
						Log.d(TAG, "Setting bytes_send param using (" + text + ") for the sequence " + seqId + " of the thread " + threadId);
						mTestingThreads.get(threadId).mTestingSequences.get(seqId).bytes_send = text.isEmpty() ? 0 : Integer.parseInt(text);
					}
				}
			});

			bytesRow.addView(bytesTextView);
			bytesRow.addView(bytesEdit);

			sequenceParametersTableLayout.addView(bytesRow);
		}

		///////////////////////////////////////////////////////////////////////
		//repeat layout
		//////////////////////////////////////////////////////////////////////
		TableRow repeatRow = new TableRow(mContext);
		repeatRow.setId(sequenceControlsId + 15);

		TextView repeatView = new TextView(mContext);
		repeatView.setId(sequenceControlsId + 16);
		EditText repeatEdit = new EditText(mContext);
		repeatEdit.setId(sequenceControlsId + 17);

		{
			repeatView.setText("Repeat");
			repeatEdit.setEms(10);
			repeatEdit.setSingleLine(true);
			repeatEdit.setInputType(InputType.TYPE_CLASS_NUMBER);
			repeatEdit.setFilters(mFilterArray);
			if (repeat != -1)
				repeatEdit.setText(Integer.toString(repeat));
			repeatEdit.addTextChangedListener(mEditSequenceChangeTextWatcher);
			repeatEdit.setOnFocusChangeListener(new OnFocusChangeListener()
			{

				public void onFocusChange(View v, boolean hasFocus)
				{
					if (!hasFocus)
					{
						EditText edit = (EditText) v;
						int editId = edit.getId();

						int threadId = (editId / MAGIC_THREAD_ID) - 1;
						int basicControlsId = (threadId + 1) * MAGIC_THREAD_ID;

						int seqId = (editId - basicControlsId) / MAGIC_SEQ_ID - 1;

						String text = edit.getText().toString();
						Log.d(TAG, "Setting repeat param using (" + text + ") for the sequence " + seqId + " of the thread " + threadId);
						mTestingThreads.get(threadId).mTestingSequences.get(seqId).repeat = text.isEmpty() ? -1 : Integer.parseInt(text);
					}
				}
			});

			repeatRow.addView(repeatView);
			repeatRow.addView(repeatEdit);

			sequenceParametersTableLayout.addView(repeatRow);
		}

		sequenceLinearLayout.addView(sequenceParametersTableLayout);

		threadLinearLayout.addView(sequenceLinearLayout);

		mConfigurationChanged = true;
		mSaveMenuItem.setVisible(true);
	}

	OnClickListener	deleteSequenceButtonListener	= new OnClickListener()
													{
														public void onClick(View v)
														{
															int buttonId = v.getId();

															mDeleteThreadId = (buttonId / MAGIC_THREAD_ID) - 1;
															int basicControlsId = (mDeleteThreadId + 1) * MAGIC_THREAD_ID;

															mDeleteSequenceId = (buttonId - basicControlsId) / MAGIC_SEQ_ID - 1;

															AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mContext);

															// set title
															alertDialogBuilder.setTitle("Sequence deletion");

															// set dialog message
															if (mTestingThreads.get(mDeleteThreadId).mTestingSequences.size() == 1)
															{
																alertDialogBuilder.setMessage("This is the only sequence in the thread.\nDelete entire Thread "
																		+ (mDeleteThreadId + 1) + "?");

																alertDialogBuilder.setPositiveButton("Yes", new DialogInterface.OnClickListener()
																{
																	public void onClick(DialogInterface dialog, int id)
																	{
																		removeThreadAt(mDeleteThreadId);
																	}
																});
																alertDialogBuilder.setNeutralButton("Sequence only", new DialogInterface.OnClickListener()
																{
																	public void onClick(DialogInterface dialog, int id)
																	{
																		removeSequenceAt(mDeleteThreadId, mDeleteSequenceId);
																	}
																});
															}
															else
															{
																alertDialogBuilder.setMessage("Delete Sequence " + (mDeleteSequenceId + 1) + "?");

																alertDialogBuilder.setPositiveButton("Yes", new DialogInterface.OnClickListener()
																{
																	public void onClick(DialogInterface dialog, int id)
																	{
																		removeSequenceAt(mDeleteThreadId, mDeleteSequenceId);

																	}
																});
															}

															alertDialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener()
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

															// show it
															alertDialog.show();

														}
													};

	private void removeSequenceAt(int threadId, int seqId)
	{
		int basicControlsId = (threadId + 1) * MAGIC_THREAD_ID;

		Log.d(TAG, "Deleting sequence for thread with id " + threadId + " sequence id " + seqId);

		LinearLayout threadLinearLayout = (LinearLayout) mConfigurationLinearLayout.findViewById(basicControlsId + THREAD_LAYOUT_ID);
		int sequenceControlsId = basicControlsId + (seqId + 1) * MAGIC_SEQ_ID;

		for (int i = 1; i <= 17; i++)
			threadLinearLayout.removeView(threadLinearLayout.findViewById(sequenceControlsId + i));

		mTestingThreads.get(threadId).mTestingSequences.remove(seqId);

		mConfigurationChanged = true;
		mSaveMenuItem.setVisible(true);
	}

	private boolean loadXmlConfiguration(String filePath)
	{
		Toast.makeText(mContext, "Loading configuration from the XML file", Toast.LENGTH_LONG).show();

		if (filePath.isEmpty())
		{
			Toast.makeText(mContext, "File is empty", Toast.LENGTH_LONG).show();
			return false;
		}

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		File file = new File(filePath);

		try
		{
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document dom = builder.parse(file);

			Element root = dom.getDocumentElement();
			root.normalize();

			NodeList threadNodes = root.getElementsByTagName("network_thread");

			if (threadNodes.getLength() == 0)
			{

				Log.d(TAG, "No network threads nodes were found in the XML file.");
				Toast.makeText(mContext, "No network threads nodes were found in the XML file.", Toast.LENGTH_SHORT).show();
				return false;
			}

			//mTestingThreads = new SparseArray<TestingThread>(threadNodes.getLength());

			for (int i = 0; i < threadNodes.getLength(); i++)
			{
				Node threadNode = threadNodes.item(i);

				if (threadNode.getNodeType() == Node.ELEMENT_NODE)
				{
					Element eThread = (Element) threadNode;

					NodeList sequenceNodes = eThread.getElementsByTagName("sequence");

					if (sequenceNodes.getLength() == 0)
					{

						Log.d(TAG, "No test sequence nodes were found in the thread node.");
						Toast.makeText(mContext, "No test sequence nodes were found in the thread node.", Toast.LENGTH_SHORT).show();
						return false;
					}

					addThread(i);

					for (int j = 0; j < sequenceNodes.getLength(); j++)
					{
						Node sequence = sequenceNodes.item(j);
						if (sequence.getNodeType() == Node.ELEMENT_NODE)
						{
							Element eElement = (Element) sequence;

							String temp = eElement.getAttribute("time_total_ms");
							int time_total = temp.isEmpty() ? 1000 : Integer.parseInt(temp);

							temp = eElement.getAttribute("bytes");
							int bytes_send = temp.isEmpty() ? 1000 : Integer.parseInt(temp);

							temp = eElement.getAttribute("delay_ns");
							long delay_ns = temp.isEmpty() ? 100*1000000 : Long.parseLong(temp);

							temp = eElement.getAttribute("repeat");
							int repeat = temp.isEmpty() ? -1 : Integer.parseInt(temp);

							addSequence(i, j, time_total, bytes_send, delay_ns, repeat);
						}

					}

				}
			}
		}
		catch (ParserConfigurationException ex)
		{
			Log.d(TAG, "Exception in Parser Configuration");
			Toast.makeText(mContext, "Exception in Parser Configuration", Toast.LENGTH_SHORT).show();
			ex.printStackTrace();
			return false;
		}
		catch (DOMException ex1)
		{
			Log.d(TAG, "DOM exception. Malformed or empty XML configuration file.");
			Toast.makeText(mContext, "DOM exception. Malformed configuration XML file.", Toast.LENGTH_SHORT).show();
			ex1.printStackTrace();
			return false;
		}
		catch (SAXException ex1)
		{
			Log.d(TAG, "SAX exception. Malformed or empty XML configuration file.");
			Toast.makeText(mContext, "SAX exception. Malformed configuration XML file.", Toast.LENGTH_SHORT).show();
			ex1.printStackTrace();
			return false;
		}
		catch (IOException ex2)
		{
			Log.d(TAG, "Can't open configuration XML file.");
			Toast.makeText(mContext, "Can't open configuration XML file.", Toast.LENGTH_SHORT).show();
			ex2.printStackTrace();
			return false;
		}

		return true;

	}

	private void saveXmlConfiguration()
	{
		Toast.makeText(mContext, "Saving configuration to the XML file", Toast.LENGTH_SHORT).show();

		if (checkConfigurationValid(mTestingThreads, mContext))
		{
			String filepath = mConfigurationFilePathEdit.getText().toString();
			if (checkFileExt(filepath, "xml"))
			{
				File file = new File(filepath);

				if (checkFilePathValid(file))
				{

					//save config to XML

					FileOutputStream fileos = null;
					try
					{
						fileos = new FileOutputStream(file);
						System.out.println("file output stream created");

					}
					catch (FileNotFoundException e)
					{
						Log.e("FileNotFoundException", e.toString());
					}
					XmlSerializer serializer = Xml.newSerializer();
					try
					{
						serializer.setOutput(fileos, "UTF-8");
						serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
						serializer.startDocument(null, Boolean.valueOf(true));

						serializer.startTag(null, "test_configuration");

						int threadsNumber = mTestingThreads.size();
						for (int index = 0; index < threadsNumber; index++)
						{
							serializer.startTag(null, "network_thread");
							
							serializer.attribute("", "seq_id", Integer.toString(index + 1) );
							
							TestingThread thread = mTestingThreads.valueAt(index);
							
							int sequencesNumber = thread.mTestingSequences.size();

							for (int seqIndex = 0; seqIndex < sequencesNumber; seqIndex++)
							{
								serializer.startTag(null, "sequence");
								
								TestingSequence sequence = thread.mTestingSequences.valueAt(seqIndex);
								
								serializer.attribute("", "seq_id", Integer.toString(seqIndex + 1) );
								
								serializer.attribute("", "time_total_ms", Integer.toString(sequence.time_total) );
								serializer.attribute("", "delay_ns", Long.toString( sequence.delay_nano) );
								serializer.attribute("", "bytes", Integer.toString(sequence.bytes_send) );
								
								if(sequence.repeat > 0)
									serializer.attribute("", "repeat", Integer.toString(sequence.repeat) );
								
								serializer.endTag(null, "sequence");
							}
							
							serializer.endTag(null, "network_thread");
						}
						
							serializer.endTag(null, "test_configuration");
						serializer.endDocument();
						serializer.flush();
						fileos.close();
					}
					catch (Exception e)
					{
						Log.e("Exception", "Exception occured in wroting");
					}

					mConfigurationChanged = false;
					mSaveMenuItem.setVisible(false);

				}
				else
				{
					Toast.makeText(mContext, "Can't save configuration using the path\n" + filepath + "", Toast.LENGTH_LONG).show();
				}
			}
			else
			{
				Toast.makeText(mContext, "Set file extension as .xml or .XML", Toast.LENGTH_LONG).show();
			}
		}

		//check whether path exists;
		String filepath = mConfigurationFilePathEdit.getText().toString();

		File file = new File(filepath);

		if (file.exists())
		{

		}
		else
		{
			try
			{
				if (file.createNewFile())
				{
					file.delete();
				}

				mConfigurationChanged = false;
				mSaveMenuItem.setVisible(false);

				return;
			}
			catch (IOException e)
			{
				return;
			}
		}

	}

	private static boolean checkFilePathValid(File file)
	{
		if (file == null)
			return false;

		boolean isValid = true;

		if (!file.exists())
		{
			try
			{
				//try to create a file with a given path,
				//if we fail - path is invalid
				if (file.createNewFile())
				{
					file.delete();
				}
			}
			catch (IOException e)
			{
				isValid = false;
			}
		}
		return isValid;
	}

	private static boolean checkConfigurationValid(SparseArray<TestingThread> testingThreads, Context context)
	{
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);

		// set title
		alertDialogBuilder.setTitle("Error in configuration");

		alertDialogBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener()
		{
			public void onClick(DialogInterface dialog, int id)
			{
			}
		});

		if (testingThreads == null)
		{
			// set dialog message
			alertDialogBuilder.setMessage("Configuration contains no threads");
			// create alert dialog and show it
			AlertDialog alertDialog = alertDialogBuilder.create();
			alertDialog.show();
			return false;
		}

		int threadsNumber = testingThreads.size();

		if (threadsNumber < 1)
		{
			// set dialog message
			alertDialogBuilder.setMessage("Configuration should contain at least one thread");
			// create alert dialog and show it
			AlertDialog alertDialog = alertDialogBuilder.create();
			alertDialog.show();
			return false;
		}

		if (threadsNumber > 10)
		{
			// set dialog message
			alertDialogBuilder.setMessage("Configuration can contain 10 threads maximum\nCurrent count " + threadsNumber);
			// create alert dialog and show it
			AlertDialog alertDialog = alertDialogBuilder.create();
			alertDialog.show();
			return false;
		}

		for (int index = 0; index < threadsNumber; index++)
		{
			TestingThread thread = testingThreads.valueAt(index);
			int key = testingThreads.keyAt(index);

			if (thread.mTestingSequences == null)
			{
				// set dialog message
				alertDialogBuilder.setMessage("Thread " + key + " contains no sequences");
				// create alert dialog and show it
				AlertDialog alertDialog = alertDialogBuilder.create();
				alertDialog.show();
				return false;
			}

			int sequencesNumber = thread.mTestingSequences.size();

			if (sequencesNumber < 1)
			{
				// set dialog message
				alertDialogBuilder.setMessage("Thread " + key + " should contain at least one sequence");
				// create alert dialog and show it
				AlertDialog alertDialog = alertDialogBuilder.create();
				alertDialog.show();
				return false;
			}

			if (sequencesNumber > 40)
			{
				// set dialog message
				alertDialogBuilder.setMessage("Thread " + key + " can contain 40 sequences maximum\nCurrent count " + sequencesNumber);
				// create alert dialog and show it
				AlertDialog alertDialog = alertDialogBuilder.create();
				alertDialog.show();
				return false;
			}

			for (int seqIndex = 0; seqIndex < sequencesNumber; seqIndex++)
			{
				TestingSequence sequence = thread.mTestingSequences.valueAt(index);
				int seqKey = thread.mTestingSequences.keyAt(index);

				if (sequence.time_total < 10)
				{
					alertDialogBuilder.setMessage("Sequence " + seqKey + " from the thread " + key + "\n" +
							"Total time can\'t be less than 10 ms");
					// create alert dialog and show it
					AlertDialog alertDialog = alertDialogBuilder.create();
					alertDialog.show();
					return false;
				}

				if (sequence.delay_nano < 1)
				{
					alertDialogBuilder.setMessage("Sequence " + seqKey + " from the thread " + key + "\n" +
							"Delay can\'t be less than 1 ns");
					// create alert dialog and show it
					AlertDialog alertDialog = alertDialogBuilder.create();
					alertDialog.show();
					return false;
				}

				if (sequence.repeat == 0)
				{
					alertDialogBuilder.setMessage("Sequence " + seqKey + " from the thread " + key + "\n" +
							"Repeat parameter can't be zero\n" +
							"Leave blank edit to set repeats to infinity\n" +
							"or enter number of repeats");
					// create alert dialog and show it
					AlertDialog alertDialog = alertDialogBuilder.create();
					alertDialog.show();
					return false;
				}
			}
		}

		return true;
	}

	private void clearEditor()
	{
		//we need to delete all sequences first
		while (mTestingThreads.size() != 0)
		{
			int key = mTestingThreads.keyAt(0);
			removeThreadAt(key);
		}
	}

	TextWatcher	mEditConfigFileChangeTextWatcher	= new TextWatcher()
													{
														@Override
														public void onTextChanged(CharSequence s, int start, int before, int count)
														{

														}

														@Override
														public void beforeTextChanged(CharSequence s, int start, int count, int after)
														{
														}

														@Override
														public void afterTextChanged(Editable s)
														{
															mAddNewThreadButton.setEnabled(true);
														}
													};

	TextWatcher	mEditSequenceChangeTextWatcher		= new TextWatcher()
													{
														@Override
														public void onTextChanged(CharSequence s, int start, int before, int count)
														{
														}

														@Override
														public void beforeTextChanged(CharSequence s, int start, int count, int after)
														{
														}

														@Override
														public void afterTextChanged(Editable s)
														{
															mConfigurationChanged = true;
															mSaveMenuItem.setVisible(true);
														}
													};

	public boolean checkFileExt(String filepath, String checkExt)
	{
		String ext = filepath.substring((filepath.lastIndexOf(".") + 1), filepath.length());
		if (ext.compareToIgnoreCase(checkExt) != 0)
			return false;
		return true;
	}

	private int getPixels(float dipValue)
	{
		return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dipValue, getResources().getDisplayMetrics());
	}
}
