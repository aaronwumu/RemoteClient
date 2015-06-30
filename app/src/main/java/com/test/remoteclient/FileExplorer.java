package com.test.remoteclient;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;

public class FileExplorer extends Activity {
	public static final String LOG_TAG					= "FileExplorer";
	
	public static final String ROOT_PATH = "/"; 
	public static final String UP_DIR = "../"; 
	public static final String SD_DIR = "/sdcard"; 
	
	public static final int FILE_TYPE_APK				= 0;
	public static final int FILE_TYPE_AUDIO 			= 1;
	public static final int FILE_TYPE_VIDEO 			= 2;
	public static final int FILE_TYPE_IMAGE 			= 3;
	public static final int FILE_TYPE_TEXT				= 4;
	public static final int FILE_TYPE_PDF				= 5;
	public static final int FILE_TYPE_CHM				= 6;
	public static final int FILE_TYPE_WORD				= 7;
	public static final int FILE_TYPE_EXCEL				= 8;
	public static final int FILE_TYPE_PPT				= 9;
	public static final int FILE_TYPE_HTML				= 10;
	public static final int FILE_TYPE_ZIP				= 11;
	public static final int FILE_TYPE_DEFAULT			= 100;//no support
	
	public static final int FILE_SHOW_MODE_LIST			= 0;
	public static final int FILE_SHOW_MODE_GRID			= 1;
	
	private static final int FILE_NORMAL_ITEM_CUT		= 0;
	private static final int FILE_NORMAL_ITEM_COPY		= 1;
	private static final int FILE_NORMAL_ITEM_RENAME	= 2;
	private static final int FILE_NORMAL_ITEM_DELETE	= 3;
	private static final int FILE_NORMAL_ITEM_PROPERTY	= 4;
	
	private static final int FILE_PASTE_ITEM_CUT		= 0;
	private static final int FILE_PASTE_ITEM_COPY		= 1;
	private static final int FILE_PASTE_ITEM_PASTE		= 2;
	private static final int FILE_PASTE_ITEM_RENAME		= 3;
	private static final int FILE_PASTE_ITEM_DELETE		= 4;
	private static final int FILE_PASTE_ITEM_PROPERTY	= 5;
	
	private static final int FILE_OPTION_NORMAL			= 0;
	private static final int FILE_OPTION_CUT			= 1;
	private static final int FILE_OPTION_COPY			= 2;
	
	private File mOptionFile;//cut or copy
	private File mRenameFile;
	private EditText mRenameEdit;
	private Dialog mRenameDialog;
	private ImageButton mModeButton;
	private ImageButton mReturn;
	private ImageButton mHome;
	private TextView mTextViewDirPath;
	private View mListLayout;
	private ListView mFileListView;
	private GridView mFileGridView;
	private FileExplorerAdapter mListItemAdapter;
	//all files(include dir) in current dir
	private ArrayList<String> mPaths = null;
    private String mCurPath = SD_DIR; 
    private int mCurPos;
    private int mFileOption = FILE_OPTION_NORMAL;
    
    private int mShowMode = FILE_SHOW_MODE_GRID;//default is grid
    //private int mShowMode = FILE_SHOW_MODE_LIST;
    
    private final BroadcastReceiver mFileReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
        	//Log.v(LOG_TAG, "file BroadcastReceiver onReceive:" + intent);
            String action = intent.getAction();
			if(action.equals(Intent.ACTION_MEDIA_EJECT)){//plug out sdcard
				Toast.makeText(FileExplorer.this, R.string.sd_plug_out, Toast.LENGTH_LONG).show();
				FileExplorer.this.finish();
			}
        }
    };
    
    private OnClickListener mClickListener = new OnClickListener(){
        public void onClick(View v){
        	switch(v.getId()){
    	        case R.id.imageButtonHome:{
    	        	//start home activity
            		Intent mIntent= new Intent(Intent.ACTION_MAIN);
                	mIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK); 
                	mIntent.addCategory(Intent.CATEGORY_HOME);
                	startActivity(mIntent);
    	        }
    	        break;
    	        
    	        case R.id.imageButtonReturn:{
    	        	FileExplorer.this.finish();
    	        }
    	        break;
    	        
    	        default:
    	        break;
            }
        }
    };
    
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        //no title and no status bar
		requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
        					WindowManager.LayoutParams.FLAG_FULLSCREEN);
        
        setContentView(R.layout.file_explorer);
        
        mReturn = (ImageButton)findViewById(R.id.imageButtonReturn);
        mHome = (ImageButton)findViewById(R.id.imageButtonHome);
        mModeButton = (ImageButton)findViewById(R.id.imageButtonGridList);
        mTextViewDirPath = (TextView)findViewById(R.id.textViewDirPath);
        mFileGridView = (GridView)findViewById(R.id.gridViewFileExplorer);

        mReturn.setOnClickListener(mClickListener);
        mHome.setOnClickListener(mClickListener);
        mModeButton.setOnTouchListener(mButtonListener);
    }

    @Override
    public void onResume() {
    	super.onResume();
    	
    	//register sdcard plug out 
		IntentFilter intentFilter = new IntentFilter(Intent.ACTION_MEDIA_EJECT);
        intentFilter.addDataScheme("file");
        registerReceiver(mFileReceiver, intentFilter);
        
    	if (!Environment.getExternalStorageState().equals("mounted")){
        	Toast.makeText(FileExplorer.this, R.string.no_sd, Toast.LENGTH_LONG).show();
        	finish();
        }
        else{
        	entryDir(mCurPath);
        }
    }
    
    @Override
	public void onPause() {
        super.onPause();

		unregisterReceiver(mFileReceiver);
    }
    
    private OnTouchListener mButtonListener = new OnTouchListener(){
		//@Override   
        public boolean onTouch(View v, MotionEvent event) {   
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_MOVE:
                {}
                break;
                    
                case MotionEvent.ACTION_UP:{
                	int grid_button_state[] = { 
        				R.drawable.img_btn_grid,
                        R.drawable.img_btn_grid, 
                        R.drawable.img_btn_grid_pressed 
        			};
                	
                	int list_button_state[] = { 
        				R.drawable.img_btn_list,
                        R.drawable.img_btn_list, 
                        R.drawable.img_btn_list_pressed 
        			};
                	
                	CustomButton customButton = new CustomButton(FileExplorer.this);

                	if( FILE_SHOW_MODE_GRID == mShowMode ){
                		mShowMode = FILE_SHOW_MODE_LIST;
                		mModeButton.setBackgroundDrawable(customButton.setbg(list_button_state));

                		mFileGridView.setAdapter(null);
                        ViewGroup root = (ViewGroup) findViewById(R.id.relativeLayoutFileShowMode);
                        getLayoutInflater().inflate(R.layout.file_explorer_list, root);
                        mListLayout = findViewById(R.id.relativeLayoutFileShowList);
                        mFileListView = (ListView)mListLayout.findViewById(R.id.listViewFileExplorer);

                        entryDir(mCurPath);
                        mListLayout.setVisibility(View.VISIBLE);
                	}
                	else{
                		mShowMode = FILE_SHOW_MODE_GRID;
                		mModeButton.setBackgroundDrawable(customButton.setbg(grid_button_state));
                		
                		mFileListView.setAdapter(null);
                		mFileListView = null;
                		mListLayout.setVisibility(View.GONE);
                		mListLayout = null;
                		
                		entryDir(mCurPath);
                	}
                }
                break;
            }
            return false;   
        } 
    };
    
    /*
    	back to parent directory
    */
    private void upDir(String curPath){
    	String upPath;
    	
    	if(!curPath.equals(ROOT_PATH)){
			if( 0 == curPath.lastIndexOf('/') ){
				upPath = ROOT_PATH;
			}
			else{
				upPath = curPath.substring(0, curPath.lastIndexOf('/'));
			}
			entryDir(upPath);
		}
    }
    
    private ArrayList<String> sortPaths(ArrayList<String> paths){
    	if( null == paths ){
    		return null;
    	}
    	
    	ArrayList<String> pathSort = new ArrayList<String>(); 
    	ArrayList<String> pathDir = new ArrayList<String>(); 
    	ArrayList<String> pathFile = new ArrayList<String>(); 
    	
    	for( int i = 0; i < paths.size(); i++ ){
    		String name = paths.get(i); 
    		if(name.equals(UP_DIR)){
    			pathDir.add(name); 
    		}
    		else{
    			File f = new File(name);  
    			if(f.isDirectory()){
    				pathDir.add(name);
    			}
    			else{
    				pathFile.add(name);
    			}
    		}
    	}
    	
    	pathSort.addAll(pathDir);
    	pathSort.addAll(pathFile);
    	return pathSort;
    }
    
    private void entryDir(String filePath) {   
        try{   
        	mPaths = new ArrayList<String>();   
            File f = new File(filePath);   
            File[] files = f.listFiles();
  
            if(files != null){   		
            	//current is root path
                if(!filePath.equals(ROOT_PATH)){ 
		            mPaths.add(UP_DIR); 
                }
                
                for (int i = 0; i < files.length; i++) {     
		            mPaths.add(files[i].getPath());   
                }   
                
                mPaths = sortPaths(mPaths);
               
                if( FILE_SHOW_MODE_GRID == mShowMode ){
                	mListItemAdapter = new FileExplorerAdapter(this, mPaths, FILE_SHOW_MODE_GRID); 
                    mFileGridView.setAdapter(mListItemAdapter);  
                    mFileGridView.setOnItemClickListener(mItemListener);
                    mFileGridView.setOnItemLongClickListener(mItemLongListener);
                    
                    Log.v(LOG_TAG, "file grid view adapter = " + mListItemAdapter);
            	}
            	else{
            		if( null == mListLayout ){
            			Log.v(LOG_TAG, "list layout not inflate !!!");
            			return;
            		}
            		mListItemAdapter = new FileExplorerAdapter(this, mPaths, FILE_SHOW_MODE_LIST); 
                    mFileListView.setAdapter(mListItemAdapter);  
                    mFileListView.setOnItemClickListener(mItemListener);
                    mFileListView.setOnItemLongClickListener(mItemLongListener);
                    
                    Log.v(LOG_TAG, "file list view adapter = " + mListItemAdapter);
            	}
            }   
        }
        catch(Exception ex){   
            ex.printStackTrace();   
        }  
        
        mCurPath = filePath;
        //setTitle(filePath);
        mTextViewDirPath.setText(filePath);
    }   
    
    OnItemClickListener mItemListener = new OnItemClickListener() {   
        @Override  
        public void onItemClick(AdapterView<?> l, View v, int position, long id) {   
        	Log.v(LOG_TAG, "onItemClick, position = " + position);
        	
        	if(( 0 == position ) && (!mCurPath.equals(ROOT_PATH))){
            	upDir(mCurPath);
            }
            else{
            	 String path = mPaths.get(position);   
                 File file = new File(path);   
          
                 if(file.isDirectory()){   
                	 entryDir(path); 
                 }
                 else{   //is a file
                	 //processFile(path);
                	 FileExplorer.this.setResult(RESULT_OK, (new Intent())
                             .putExtra("file_path", path));
                	 finish();
                 } 
            }
        }   
    };
    
    OnItemLongClickListener mItemLongListener = new OnItemLongClickListener() {   
        @Override  
        public boolean onItemLongClick(AdapterView<?> l, View v, int position, long id) {   
        	Log.v(LOG_TAG, "onItemLongClick, position = " + position);
        	
        	mCurPos = position;
        	if( FILE_OPTION_NORMAL == mFileOption ){
        		fileNormalOption();
        	}
        	else{
        		filePasteOption();
        	}
        	return true;
        }   
    };
    
    private void fileNormalOption(){
    	CharSequence itemCut = getString(R.string.file_cut);
    	CharSequence itemCopy = getString(R.string.file_copy);
    	CharSequence itemRename = getString(R.string.file_rename);
    	CharSequence itemDelete = getString(R.string.file_delete);
    	CharSequence itemProperty = getString(R.string.file_property);
    	new AlertDialog.Builder(FileExplorer.this)
    	.setTitle(R.string.file_operation)
        .setItems(new CharSequence[] {itemCut, itemCopy, itemRename, itemDelete, itemProperty},
        		new DialogInterface.OnClickListener() {
            		public void onClick(DialogInterface dialog, int which) {
            			dialog.dismiss();
                        switch (which) {
                        	case FILE_NORMAL_ITEM_CUT:		fileCut();		break;
                        	case FILE_NORMAL_ITEM_COPY:		fileCopy();		break;
                        	case FILE_NORMAL_ITEM_RENAME:	fileRename();	break;
                        	case FILE_NORMAL_ITEM_DELETE:	fileDelete();	break;
                        	case FILE_NORMAL_ITEM_PROPERTY:fileProperty();	break;
                        	default:
                        	break;
                        }
            		}
        		})
        .show();
    }
    
    private void filePasteOption(){
    	CharSequence itemCut = getString(R.string.file_cut);
    	CharSequence itemCopy = getString(R.string.file_copy);
    	CharSequence itemPaste = getString(R.string.file_paste);
    	CharSequence itemRename = getString(R.string.file_rename);
    	CharSequence itemDelete = getString(R.string.file_delete);
    	CharSequence itemProperty = getString(R.string.file_property);
    	new AlertDialog.Builder(FileExplorer.this)
    	.setTitle(R.string.file_operation)
        .setItems(new CharSequence[] {itemCut, itemCopy, itemPaste, itemRename, itemDelete, itemProperty},
        		new DialogInterface.OnClickListener() {
            		public void onClick(DialogInterface dialog, int which) {
            			dialog.dismiss();
                        switch (which) {
                        	case FILE_PASTE_ITEM_CUT:		fileCut();		break;
                        	case FILE_PASTE_ITEM_COPY:		fileCopy();		break;
                        	case FILE_PASTE_ITEM_PASTE:		filePaste();	break;
                        	case FILE_PASTE_ITEM_RENAME:	fileRename();	break;
                        	case FILE_PASTE_ITEM_DELETE:	fileDelete();	break;
                        	case FILE_PASTE_ITEM_PROPERTY:fileProperty();	break;
                        	default:
                        	break;
                        }
            		}
        		})
        .show();
    }
    
    private void fileCut(){
    	mFileOption = FILE_OPTION_CUT;
    	mOptionFile = new File(mPaths.get(mCurPos));  
    }
    
    private void fileCopy(){
    	mFileOption = FILE_OPTION_COPY;
    	mOptionFile = new File(mPaths.get(mCurPos));  
    }
    
    private void filePaste(){
    	String newFileName = mCurPath + "/" + mOptionFile.getName();
    	File newFile = new File(newFileName);
    	if( FILE_OPTION_CUT == mFileOption ){
    		if(mOptionFile.isDirectory()){   
            	try{
            		FileUtils.moveDirectory(mOptionFile, newFile);
            	}catch(IOException e){}
            }
            else {   //is a file
            	try{
            		FileUtils.moveFile(mOptionFile, newFile);
            	}catch(IOException e){}
            } 
    	}
    	else if( FILE_OPTION_COPY == mFileOption ){
    		if(mOptionFile.isDirectory()){   
            	try{
            		FileUtils.copyDirectory(mOptionFile, newFile);
            	}catch(IOException e){}
            }
            else{   //is a file
            	try{
            		FileUtils.copyFile(mOptionFile, newFile);
            	}catch(IOException e){}
            } 
    	}
    	
    	entryDir(mCurPath);
    	mFileOption = FILE_OPTION_NORMAL;
    	
    	Toast.makeText(FileExplorer.this, R.string.success, Toast.LENGTH_LONG).show();
    }
    
    private void fileRename(){
    	LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);  
    	View v = inflater.inflate(R.layout.file_rename, null); 
    	
    	mRenameEdit = (EditText) v.findViewById(R.id.editTextRename);  
    	mRenameFile = new File(mPaths.get(mCurPos));  
    	mRenameEdit.setText(mRenameFile.getName()); 
    	
    	Button buttonOK = (Button) v.findViewById(R.id.buttonOK);  
    	Button buttonCancel = (Button) v.findViewById(R.id.buttonCancel);  
    	
    	buttonOK.setOnClickListener(new OnClickListener(){
		    		public void onClick(View v) {
		    			int stringID = R.string.success;
		    			
		    			if( null != mRenameDialog ){
		    				mRenameDialog.dismiss();
		    				mRenameDialog = null;
		    			}
		    			
		    			String newFileName = String.valueOf(mRenameEdit.getText());
		    			if(!mRenameFile.getName().equals(newFileName)){
		    				String curPath = mPaths.get(mCurPos);
			    			newFileName = curPath.substring(0, curPath.lastIndexOf('/') + 1) + newFileName;

			    			if(mRenameFile.renameTo(new File(newFileName))){
			    				stringID = R.string.success;
			    				entryDir(mCurPath);
			    			}
			    			else{
			    				stringID = R.string.fail;
			    			}
		    			}
		    			
		    			Toast.makeText(FileExplorer.this, stringID, Toast.LENGTH_LONG).show();
		    		}
    			});
    	
    	buttonCancel.setOnClickListener(new OnClickListener(){
    		public void onClick(View v) {
    			if( null != mRenameDialog ){
    				mRenameDialog.dismiss();
    				mRenameDialog = null;
    			}
    		}
		});
    	
    	mRenameDialog = new AlertDialog.Builder(FileExplorer.this)
				        .setView(v)
				        .show();
    }
    
    private void fileDelete(){
    	int stringID = R.string.success;
    	String path = mPaths.get(mCurPos);   
        File file = new File(path);
        
        if(file.isDirectory()){   
        	try{
        		FileUtils.deleteDirectory(file);
        	}catch(IOException e){}
        	entryDir(mCurPath);  
        }
        else{   //is a file
        	if(file.delete()){
            	stringID = R.string.success;
            	entryDir(mCurPath); 
            }
            else{
            	stringID = R.string.fail;
            }
        } 

        Toast.makeText(FileExplorer.this, stringID, Toast.LENGTH_LONG).show();
    }
    
    private void fileProperty(){
    	String text;
		long size;
		
    	LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);  
    	View v = inflater.inflate(R.layout.file_property, null); 
    	ImageView imageViewFileType = (ImageView) v.findViewById(R.id.imageViewFileProperty);   
    	TextView textViewFileName = (TextView) v.findViewById(R.id.textViewFileName);  
    	
    	TextView textViewFileType = (TextView) v.findViewById(R.id.textViewType);  
    	
    	TextView textViewLocation = (TextView) v.findViewById(R.id.textViewLocation);  
    	TextView textViewSize = (TextView) v.findViewById(R.id.textViewSize);  
    	
    	TextView textViewLastModified = (TextView) v.findViewById(R.id.textViewLastModified);  
    	
    	TextView textViewReadable = (TextView) v.findViewById(R.id.textViewReadable);  
    	TextView textViewWriteable = (TextView) v.findViewById(R.id.textViewWriteable);  
    	TextView textViewHidden = (TextView) v.findViewById(R.id.textViewHidden);  
    	
    	//file size
    	File f = new File(mPaths.get(mCurPos));  
		if(f.isDirectory()){
			imageViewFileType.setImageDrawable(getResources().getDrawable(R.drawable.img_folder));  
			textViewFileType.setText(getString(R.string.folder));
			
			size = FileUtils.sizeOfDirectory(f);
		}
		else{
			imageViewFileType.setImageDrawable(mListItemAdapter.getFileDrawable(f));
			textViewFileType.setText(getString(R.string.file));

			size = f.length();
		}
		
		if( size < 1024 ){//1KB
			text = String.format("%dB", size) + "(" + size + "Bytes)"; 
		}
		else if( (size > 1024) && (size < 1024*1024) ){//  1KB <size<1MB
			text = String.format("%dKB", size/1024) + "(" + size + "Bytes)";; 
		}
		else{// >1MB
			text = String.format("%dMB", size/1024/1024) + "(" + size + "Bytes)";; 
		}
		textViewSize.setText(text);
		
		//file name
		textViewFileName.setText(f.getName()); 
		
		//file location
		textViewLocation.setText(mPaths.get(mCurPos));
		
		//file last modified
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		textViewLastModified.setText(dateFormat.format(new Date(f.lastModified())));
		
		textViewReadable.setText("" + f.canRead());
		textViewWriteable.setText("" + f.canWrite());
		textViewHidden.setText("" + f.isHidden());
		
		new AlertDialog.Builder(FileExplorer.this)
    	.setTitle(R.string.file_property)
        .setView(v)
        .show();
    }
    
    private void processFile(String path){
    	int type = getFileType(path);
    	switch(type){    	
    		case FILE_TYPE_APK:		installApk(new File(path));		break;
    		case FILE_TYPE_AUDIO:	startAudioFile(new File(path));	break;
    		case FILE_TYPE_VIDEO:	startVideoFile(new File(path));	break;
    		case FILE_TYPE_IMAGE:	showImageFile(new File(path));	break;
    		case FILE_TYPE_TEXT:	showTextFile(new File(path));	break;
    		case FILE_TYPE_PDF:		showPdfFile(new File(path));	break;
    		case FILE_TYPE_CHM:		showChmFile(new File(path));	break;
    		case FILE_TYPE_WORD:	showWordFile(new File(path));	break;
    		case FILE_TYPE_EXCEL:	showExcelFile(new File(path));	break;
    		case FILE_TYPE_PPT:		showPptFile(new File(path));	break;
    		case FILE_TYPE_HTML:	showHtmlFile(new File(path));	break;
    		case FILE_TYPE_ZIP:		showZipFile(new File(path));	break;
    		default:break;
    	}
    }

    private int getFileType(String name){
		String ext = name.substring(name.lastIndexOf(".") + 1, name.length()).toLowerCase();

		if (ext.equals("apk")){
			return FILE_TYPE_APK;
        }
		
		if(ext.equals("rmvb") || ext.equals("rm") || ext.equals("avi") || ext.equals("mp4") || ext.equals("3gp")   || 
			ext.equals("mpeg") || ext.equals("mpg") || ext.equals("flv") || ext.equals("mkv")){
			return FILE_TYPE_VIDEO;
		}
		
		if(ext.equals("mp3") || ext.equals("mid") || ext.equals("midi") || ext.equals("wav")  || 
			ext.equals("amr") || ext.equals("ogg") || ext.equals("x-ogg") || ext.equals("aac") || ext.equals("wma")){
			return FILE_TYPE_AUDIO;
		}
		
		if(ext.equals("png") || ext.equals("jpg") || ext.equals("bmp") ||
			ext.equals("gif")  || ext.equals("jpeg")){
			return FILE_TYPE_IMAGE;
		}
		
		if(ext.equals("doc") || ext.equals("docx")){
			return FILE_TYPE_WORD;
		}
		
		if(ext.equals("ppt") || ext.equals("pps")){
			return FILE_TYPE_PPT;
		}
		
		if (ext.equals("xls")){
			return FILE_TYPE_EXCEL;
        }
		
		if (ext.equals("chm")){
			return FILE_TYPE_CHM;
        }

		if(ext.equals("html") || ext.equals("htm") || ext.equals("xml")){
			return FILE_TYPE_HTML;
		}
		
		if(ext.equals("txt") || ext.equals("text") || ext.equals("ini")){
			return FILE_TYPE_TEXT;
		}
		
		if(ext.equals("zip") || ext.equals("tar") || ext.equals("gz") ||
			ext.equals("rar")  || ext.equals("cab")){
			return FILE_TYPE_ZIP;
		}
		
		if (ext.equals("pdf")){
			return FILE_TYPE_PDF;
        }
		
		return FILE_TYPE_DEFAULT;
    }

    private void installApk(File file) {
    	// Change the system setting
    	//Settings.Secure.putInt(getContentResolver(), Settings.Secure.INSTALL_NON_MARKET_APPS, 1);
    	Intent intent = new Intent(Intent.ACTION_VIEW);
    	Uri data = Uri.fromFile(file);
    	String type = "application/vnd.android.package-archive";
    	intent.setDataAndType(data, type);
    	startActivity(intent);
	}
    
    public void showChmFile(File file){
    	Intent intent = new Intent("android.intent.action.VIEW");
    	intent.addCategory("android.intent.category.DEFAULT");
    	intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Uri localUri = Uri.fromFile(file);
        intent.setDataAndType(localUri, "application/x-chm");
        startActivity(intent);
    }

    public void showExcelFile(File file){
    	Intent intent = new Intent("android.intent.action.VIEW");
    	intent.addCategory("android.intent.category.DEFAULT");
    	intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Uri localUri = Uri.fromFile(file);
        intent.setDataAndType(localUri, "application/vnd.ms-excel");
        startActivity(intent);
    }

    public void showHtmlFile(File file){
        String path = file.getPath();
        Uri localUri = Uri.parse(path).buildUpon().encodedAuthority("com.android.htmlfileprovider").scheme("content").encodedPath(path).build();
        Intent intent = new Intent("android.intent.action.VIEW");
        intent.setDataAndType(localUri, "text/html");
        startActivity(intent);
    }

    public void showImageFile(File file){
    	Intent intent = new Intent("android.intent.action.VIEW");
    	intent.addCategory("android.intent.category.DEFAULT");
        Uri localUri = Uri.fromFile(file);
        intent.setDataAndType(localUri, "image/*");
        startActivity(intent);
    }

    public void showPdfFile(File file){
    	Intent intent = new Intent("android.intent.action.VIEW");
    	intent.addCategory("android.intent.category.DEFAULT");
    	intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Uri localUri = Uri.fromFile(file);
        intent.setDataAndType(localUri, "application/pdf");
        startActivity(intent);
    }

    public void showPptFile(File file){
    	Intent intent = new Intent("android.intent.action.VIEW");
    	intent.addCategory("android.intent.category.DEFAULT");
    	intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Uri localUri = Uri.fromFile(file);
        intent.setDataAndType(localUri, "application/vnd.ms-powerpoint");
        startActivity(intent);
    }

    public void showTextFile(File file){
    	Intent intent = new Intent("android.intent.action.VIEW");
    	intent.addCategory("android.intent.category.DEFAULT");
    	intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Uri localUri = Uri.fromFile(file);
        intent.setDataAndType(localUri, "text/plain");
        startActivity(intent);
    }

    public void showWordFile(File file){
    	Intent intent = new Intent("android.intent.action.VIEW");
    	intent.addCategory("android.intent.category.DEFAULT");
    	intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Uri localUri = Uri.fromFile(file);
        intent.setDataAndType(localUri, "application/msword");
        startActivity(intent);
    }

    public void showZipFile(File file){
    	Intent intent = new Intent("android.intent.action.VIEW");
    	intent.addCategory("android.intent.category.DEFAULT");
    	intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Uri localUri = Uri.fromFile(file);
        intent.setDataAndType(localUri, "application/zip");
        startActivity(intent);
    }

    public void startAudioFile(File file){
    	Intent intent = new Intent("android.intent.action.VIEW");
    	//intent.addCategory("android.intent.category.DEFAULT");
    	intent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY);
    	intent.putExtra("oneshot", 0);
    	intent.putExtra("configchange", 0);
        Uri localUri = Uri.fromFile(file);
        intent.setDataAndType(localUri, "audio/*");
        startActivity(intent);
    }

    public void startVideoFile(File file){
    	Intent intent = new Intent("android.intent.action.VIEW");
    	//intent.addCategory("android.intent.category.DEFAULT");
    	intent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY);
    	intent.putExtra("oneshot", 0);
    	intent.putExtra("configchange", 0);
        Uri localUri = Uri.fromFile(file);
        intent.setDataAndType(localUri, "video/*");
        startActivity(intent);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent msg) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			if(mCurPath.equals(ROOT_PATH)){
				this.finish();
			}
			else{
				upDir(mCurPath);
			}
			return true;
		} 
		return super.onKeyDown(keyCode, msg);
	}
}
