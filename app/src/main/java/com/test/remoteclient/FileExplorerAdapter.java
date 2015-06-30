package com.test.remoteclient;

import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class FileExplorerAdapter extends BaseAdapter {
	public static final String LOG_TAG					= "FileExplorerAdapter";
	public static final boolean DEBUG					= true;
	public static final boolean SIGNATURE_INFO			= false;
	
	private ArrayList<String> mPaths = null;
	private LayoutInflater mInflater;   
	private Context mContext;
	private int mShowMode;
	
	public FileExplorerAdapter(Context c, ArrayList<String> pathList, int mode){
		mPaths = pathList;   
		mContext = c;   
		mShowMode = mode;
		mInflater = (LayoutInflater) c.getSystemService(Context.LAYOUT_INFLATER_SERVICE);   
	}
	
	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		return mPaths.size();
	}

	@Override
	public Object getItem(int position) {
		// TODO Auto-generated method stub
		return mPaths.get(position);
	}

	@Override
	public long getItemId(int position) {
		// TODO Auto-generated method stub
		return position;
	}

	/*
		setTag 用View设置存储数据
		notifyDataSetChanged() 告诉View数据更改并刷新
		View convertView = mInflater.inflate(R.layout.app_info_item, null)  加载XML Item 示图
	*/
	@Override
	public View getView(int position, View v, ViewGroup parent) {
		// TODO Auto-generated method stub
		ViewHolder holder;   
		if ( null == v ) {   
			if( FileExplorer.FILE_SHOW_MODE_LIST == mShowMode ){
				//for listview adapter
				v = mInflater.inflate(R.layout.list_item, null);   
				holder = new ViewHolder();   
				holder.filePicture = (ImageView) v.findViewById(R.id.ImageViewListItem);   
				holder.filePath = (TextView) v.findViewById(R.id.TextViewListItem);    
			}
			else{
				//for gridview adapter
				v = mInflater.inflate(R.layout.grid_list_item, null);   
				holder = new ViewHolder();   
				holder.filePicture = (ImageView) v.findViewById(R.id.ImageViewListItem);   
				holder.filePath = (TextView) v.findViewById(R.id.TextViewListItem);  
			}
			v.setTag(holder);  
		} 
		else {   
			holder = (ViewHolder) v.getTag();   
		}   

		if( position < mPaths.size() ){
			String path = mPaths.get(position);   
			if(path.equals(FileExplorer.UP_DIR)){
	            holder.filePicture.setImageDrawable(mContext.getResources().getDrawable(R.drawable.img_up_dir)); 
			}
			else{
				File f = new File(path);  
				if(f.isDirectory()){
					holder.filePicture.setImageDrawable(mContext.getResources().getDrawable(R.drawable.img_folder));  
				}
				else{
					holder.filePicture.setImageDrawable(getFileDrawable(f));
				}
			}			
			//holder.filePath.setText(path); 
			holder.filePath.setText(new File(path).getName()); 
		}
		return v;   
	}

	public void remove(int position){   
		mPaths.remove(position);   
		this.notifyDataSetChanged();   
	}  

	private Drawable getUninstallAPKIcon(String apkPath) {
	    if(true){
	        return null;
	    }
	    
        PackageInfo pkgInfo = mContext.getPackageManager()
                .getPackageArchiveInfo(apkPath, PackageManager.GET_ACTIVITIES);
        
        /*
        Resources res = null;
        AssetManager assmgr = new AssetManager();
        if(0 != assmgr.addAssetPath(apkPath))
            res = new Resources(assmgr, mContext.getResources().getDisplayMetrics(),
                    mContext.getResources().getConfiguration());
        
        if(res!=null){
            try{
                return res.getDrawable(pkgInfo.applicationInfo.icon);
            }
            catch (Resources.NotFoundException resnotfound){
                return mContext.getPackageManager().getApplicationIcon(pkgInfo.applicationInfo);
            }
        }
        assmgr.close();
        */
        
        Drawable icon = null;
        ApplicationInfo info = pkgInfo.applicationInfo;
        String PATH_AssetManager = "android.content.res.AssetManager";
        
        try {
            Class[] typeArgs;
            Object[] valueArgs;
            
            Class assetMagCls = Class.forName(PATH_AssetManager);
            Constructor assetMagCt = assetMagCls.getConstructor((Class[]) null);
            Object assetMag = assetMagCt.newInstance((Object[]) null);
            typeArgs = new Class[1];
            typeArgs[0] = String.class;
            Method assetMag_addAssetPathMtd = assetMagCls.getDeclaredMethod("addAssetPath",typeArgs);
            valueArgs = new Object[1];
            valueArgs[0] = apkPath;
            assetMag_addAssetPathMtd.invoke(assetMag, valueArgs);
            Resources res = mContext.getResources();
            typeArgs = new Class[3];
            typeArgs[0] = assetMag.getClass();
            typeArgs[1] = res.getDisplayMetrics().getClass();
            typeArgs[2] = res.getConfiguration().getClass();
            Constructor resCt = Resources.class.getConstructor(typeArgs);
            valueArgs = new Object[3];
            valueArgs[0] = assetMag;
            valueArgs[1] = res.getDisplayMetrics();
            valueArgs[2] = res.getConfiguration();
            res = (Resources) resCt.newInstance(valueArgs);
            CharSequence label = null;
            if (info.labelRes != 0) {
                label = res.getText(info.labelRes);
                if(DEBUG)
                    Log.v(LOG_TAG, "label=" + label);
            }
    
            if (info.icon != 0) {
                icon = res.getDrawable(info.icon);
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
        
        return icon;
	}
	
	private Drawable getUninstallAPKIcon_old(String apkPath) {
        String PATH_PackageParser = "android.content.pm.PackageParser";
        String PATH_AssetManager = "android.content.res.AssetManager";
        Drawable icon = null;

		if(DEBUG)
			Log.v(LOG_TAG, "file name:" + apkPath);
		
        try {
                // apk包的文件路径
                // 这是一个Package 解释器, 是隐藏的
                // 构造函数的参数只有一个, apk文件的路径
                // PackageParser packageParser = new PackageParser(apkPath);
                Class pkgParserCls = Class.forName(PATH_PackageParser);
                Class[] typeArgs = new Class[1];
                typeArgs[0] = String.class;
                Constructor pkgParserCt = pkgParserCls.getConstructor(typeArgs);
                Object[] valueArgs = new Object[1];
                valueArgs[0] = apkPath;
                Object pkgParser = pkgParserCt.newInstance(valueArgs);
                //Log.v(LOG_TAG, "pkgParser:" + pkgParser.toString());
                // 这个是与显示有关的, 里面涉及到一些像素显示等等, 我们使用默认的情况
                DisplayMetrics metrics = new DisplayMetrics();
                metrics.setToDefaults();
                // PackageParser.Package mPkgInfo = packageParser.parsePackage(new
                // File(apkPath), apkPath,
                // metrics, 0);
                typeArgs = new Class[4];
                typeArgs[0] = File.class;
                typeArgs[1] = String.class;
                typeArgs[2] = DisplayMetrics.class;
                typeArgs[3] = Integer.TYPE;
                Method pkgParser_parsePackageMtd = pkgParserCls.getDeclaredMethod("parsePackage",typeArgs);
                valueArgs = new Object[4];
                valueArgs[0] = new File(apkPath);
                valueArgs[1] = apkPath;
                valueArgs[2] = metrics;
                valueArgs[3] = 0;
                Object pkgParserPkg = pkgParser_parsePackageMtd.invoke(pkgParser, valueArgs);
                // 应用程序信息包, 这个公开的, 不过有些函数, 变量没公开
                // ApplicationInfo info = mPkgInfo.applicationInfo;
                Field appInfoFld = pkgParserPkg.getClass().getDeclaredField("applicationInfo");
                ApplicationInfo info = (ApplicationInfo) appInfoFld.get(pkgParserPkg);
                // uid 输出为"-1"，原因是未安装，系统未分配其Uid。
                if(DEBUG)
                	Log.v(LOG_TAG, "pkg name:" + info.packageName + " uid=" + info.uid);
                // Resources pRes = getResources();
                // AssetManager assmgr = new AssetManager();
                // assmgr.addAssetPath(apkPath);
                // Resources res = new Resources(assmgr, pRes.getDisplayMetrics(),
                // pRes.getConfiguration());
                Class assetMagCls = Class.forName(PATH_AssetManager);
                Constructor assetMagCt = assetMagCls.getConstructor((Class[]) null);
                Object assetMag = assetMagCt.newInstance((Object[]) null);
                typeArgs = new Class[1];
                typeArgs[0] = String.class;
                Method assetMag_addAssetPathMtd = assetMagCls.getDeclaredMethod("addAssetPath",typeArgs);
                valueArgs = new Object[1];
                valueArgs[0] = apkPath;
                assetMag_addAssetPathMtd.invoke(assetMag, valueArgs);
                Resources res = mContext.getResources();
                typeArgs = new Class[3];
                typeArgs[0] = assetMag.getClass();
                typeArgs[1] = res.getDisplayMetrics().getClass();
                typeArgs[2] = res.getConfiguration().getClass();
                Constructor resCt = Resources.class.getConstructor(typeArgs);
                valueArgs = new Object[3];
                valueArgs[0] = assetMag;
                valueArgs[1] = res.getDisplayMetrics();
                valueArgs[2] = res.getConfiguration();
                res = (Resources) resCt.newInstance(valueArgs);
                CharSequence label = null;
                if (info.labelRes != 0) {
                	label = res.getText(info.labelRes);
					if(DEBUG)
                		Log.v(LOG_TAG, "label=" + label);
                }

                if (info.icon != 0) {
                	icon = res.getDrawable(info.icon);
                }

                if(!SIGNATURE_INFO)
                	return icon;
                
				typeArgs = new Class[2];
                typeArgs[0] = pkgParserPkg.getClass();
                typeArgs[1] = Integer.TYPE;
                Method pkgParser_collCertifiMtd = pkgParserCls.getDeclaredMethod("collectCertificates", typeArgs);
				valueArgs = new Object[2];
                valueArgs[0] = pkgParserPkg;
                valueArgs[1] = 0;
                Object retCerti = pkgParser_collCertifiMtd.invoke(pkgParser, valueArgs);
                Boolean changeValue = (Boolean)retCerti;
				//Log.d("FileExplorerAdapter", "collectCertificates:" + changeValue.booleanValue());

				Object intArray = Array.newInstance(Integer.TYPE, 0);
				typeArgs = new Class[5];
                typeArgs[0] = pkgParserPkg.getClass();
                typeArgs[1] = intArray.getClass();
				typeArgs[2] = Integer.TYPE;
				typeArgs[3] = Long.TYPE;
				typeArgs[4] = Long.TYPE;
                Method pkgParser_genPackageInfoMtd = pkgParserCls.getDeclaredMethod("generatePackageInfo", typeArgs);
				valueArgs = new Object[5];
                valueArgs[0] = pkgParserPkg;
                valueArgs[1] = null;
				valueArgs[2] = PackageManager.GET_SIGNATURES;
				valueArgs[3] = 0;
				valueArgs[4] = 0;
				Object retPackage = pkgParser_genPackageInfoMtd.invoke(pkgParser, valueArgs);
				PackageInfo packageInfo = (PackageInfo)retPackage;

				if( null != packageInfo.signatures ){
					int len = packageInfo.signatures.length;
					for(int i = 0; i < len; i++){
						if(DEBUG)
							Log.v(LOG_TAG, "package signature:" + packageInfo.signatures[i].toCharsString());
					}
				}
				
				//packageParser.collectCertificates(pkg, 0); 
				//PackageParser.generatePackageInfo(pkg, null, flags, 0, 0);
        } catch (Exception e) {
			e.printStackTrace();
        }
        
        return icon;
	}
	
	public Drawable getFileDrawable(File f){
    	String path = f.getPath();
		String ext = path.substring(path.lastIndexOf(".") + 1, path.length()).toLowerCase();

		if (ext.equals("apk")){
			//Log.v("FileManager", "get apk file drawable");

			/*
			PackageManager pm = mContext.getPackageManager();      
			//PackageInfo info = pm.getPackageArchiveInfo(path, PackageManager.GET_ACTIVITIES | PackageManager.GET_SIGNATURES);      
			PackageInfo info = pm.getPackageArchiveInfo(path, PackageManager.GET_SIGNATURES); 
			if( null != info )
			{      
				if( null != info.signatures ){
					int len = info.signatures.length;
					for(int i = 0; i < len; i++){
						Log.v("FileExplorerAdapter", "package signature:" + new String(info.signatures[i].toByteArray()));
					}
				}

				Resources res = null;
				res = new Resources(mContext.getAssets(), mContext.getResources().getDisplayMetrics(), mContext.getResources().getConfiguration());
				try{
					return res.getDrawable(info.applicationInfo.icon);
				}
				catch (Resources.NotFoundException resnotfound){
					ApplicationInfo appInfo = info.applicationInfo;      
				    //String appName = pm.getApplicationLabel(appInfo).toString();      
				    //String packageName = appInfo.packageName;      
				    //Drawable icon = pm.getApplicationIcon(appInfo);    
				    
				    //Log.v("FileManager", "application info:" + appInfo);
					return pm.getApplicationIcon(appInfo); 
				}
			}*/

			Drawable icon = getUninstallAPKIcon(path);
			if(null == icon){
				icon = mContext.getResources().getDrawable(R.drawable.img_file_apk);
			}
			return icon;
        }
		
		if(ext.equals("rmvb") || ext.equals("rm") || ext.equals("avi") || ext.equals("mp4") || ext.equals("3gp")  || 
			ext.equals("mpeg") || ext.equals("mpg") || ext.equals("flv") || ext.equals("mkv")){
			return mContext.getResources().getDrawable(R.drawable.img_file_video);
		}
		
		if(ext.equals("mp3") || ext.equals("mid") || ext.equals("midi") || ext.equals("wav")  || 
			ext.equals("amr") || ext.equals("ogg") || ext.equals("x-ogg") || ext.equals("aac") || ext.equals("wma")){
			return mContext.getResources().getDrawable(R.drawable.img_file_audio);
		}
		
		if(ext.equals("png") || ext.equals("jpg") || ext.equals("bmp") ||
			ext.equals("gif")  || ext.equals("jpeg")){
			return mContext.getResources().getDrawable(R.drawable.img_file_picture);
		}
		
		if(ext.equals("doc") || ext.equals("docx")){
			return mContext.getResources().getDrawable(R.drawable.img_file_word);
		}
		
		if(ext.equals("ppt") || ext.equals("pps")){
			return mContext.getResources().getDrawable(R.drawable.img_file_ppt);
		}
		
		if (ext.equals("xls")){
			return mContext.getResources().getDrawable(R.drawable.img_file_excel);
        }
		
		if (ext.equals("chm")){
			return mContext.getResources().getDrawable(R.drawable.img_file_chm);
        }

		if(ext.equals("html") || ext.equals("htm") || ext.equals("xml")){
			return mContext.getResources().getDrawable(R.drawable.img_file_html);
		}
		
		if(ext.equals("txt") || ext.equals("text") || ext.equals("ini")){
			return mContext.getResources().getDrawable(R.drawable.img_file_text);
		}
		
		if(ext.equals("zip") || ext.equals("tar") || ext.equals("gz") ||
			ext.equals("rar")  || ext.equals("cab")){
			return mContext.getResources().getDrawable(R.drawable.img_file_zip);
		}
		
		if (ext.equals("pdf")){
			return mContext.getResources().getDrawable(R.drawable.img_file_pdf);
        }
		
		return mContext.getResources().getDrawable(R.drawable.img_file_default);
	} 
	
	private class ViewHolder {   
		ImageView filePicture;   
		TextView filePath;   
	}   			
}
