package com.moyobar.app.initiator;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.lidroid.xutils.DbUtils;
import com.lidroid.xutils.exception.DbException;
import com.moyobar.app.api.ApiCallback;
import com.moyobar.app.api.Client;
import com.moyobar.app.beans.Brand;
import com.moyobar.app.beans.Category;
import com.moyobar.app.beans.City;
import com.moyobar.app.beans.Cluster;
import com.moyobar.app.beans.Store;
import com.moyobar.app.common.AppContext;
import com.moyobar.app.common.StringUtils;
import com.moyobar.app.init.R;
import com.moyobar.app.utils.FileUtils;
import com.nostra13.universalimageloader.core.ImageLoader;

public class Main extends Activity {

	MainViewHandler handler;
	TextView logView;
	DbUtils dball;
	DbUtils db;
	
	AppContext context;
	
	ImageLoader imageLoader;
	String sdcard = Environment.getExternalStorageDirectory().getAbsolutePath();
	String appdir = "/luxurylocator_cache_city_4";
	
	protected void onCreate(android.os.Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		Toast.makeText(getApplicationContext(), sdcard, Toast.LENGTH_SHORT).show();
		
		handler = new MainViewHandler();
		dball = DbUtils.create(getApplicationContext(),"citys.db");
		
		
		logView = (TextView)findViewById(R.id.initiator_log);
		
		context = (AppContext)getApplication();
		
		Button initBtn = (Button)findViewById(R.id.initiator_btn);
		initBtn.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View view) {
				
				final Message msg = new Message();
				
				new Thread(){
					public void run() {
						
						msg.what = 1;
						msg.arg1 = 1;
						msg.arg2 = 1;
						
						handler.sendMessage(msg);
						
						Client.getCities(context, new ApiCallback<City>(){
							
							public void onCompleted(ApiCallback.Result<City> result) {
								
								List<City> cities = result.getEntities();
								List<Integer> cityIds = new ArrayList<Integer>();
 								for(City city : cities){
									try {
										dball.saveOrUpdate(city);
										msg.arg1 = 2;
										msg.obj = city.getTitle();
										System.out.println(city.getLocalizedTitle()+"插入成功！..");
										cityIds.add(city.getId());
									} catch (DbException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}
								}
								
								
								//int[] cityIds = new int[]{2,1,40};
								//int[] cityIds = new int[]{33};
								for(final int cityId:cityIds){
									db = DbUtils.create(context,"city_"+cityId+".db");
									try {
										City city = dball.findById(City.class, cityId);
										city.setOffline(1);
										city.setIsLatest(1);
										db.update(city,"offline");
										
										System.out.println(city.getLocalizedTitle()+"数据加载开始");
										handler.obtainMessage(1, city.getLocalizedTitle()).sendToTarget();
									} catch (DbException e1) {
										// TODO Auto-generated catch block
										e1.printStackTrace();
									}
									
									initCluster(cityId);
									
									handler.obtainMessage(1, "商圈完成").sendToTarget();
									
									initCategory(cityId);
									handler.obtainMessage(1, "分类完成").sendToTarget();
									
									context.cityId = String.valueOf(cityId);
									
									
									Client.getBrands(context, null, null, null, null, null, new ApiCallback<Brand>(){
										
										@Override
										public void onCompleted(
												com.moyobar.app.api.ApiCallback.Result<Brand> result) {
											
											List<Brand> brands = result.getEntities();
											for(final Brand brand:brands){
												try {
													Brand oldBrand = db.findById(Brand.class, brand.getId());
													if(oldBrand == null){
														brand.setCityId(brand.getCityId()+cityId+"|");
													}else{
														if(oldBrand.getCityId().indexOf("|"+cityId+"|") == -1){
															brand.setCityId(oldBrand.getCityId()+cityId+"|");
														}
													}
													
													String fileName = brand.getLogo_150_100().substring(8);
													fileName = fileName.substring(fileName.indexOf("/"));
													final String path = sdcard+appdir+fileName;
													
													brand.setLocalLogo(fileName);
													
													new Thread(){
														
														public void run() {
															
															InputStream is = FileUtils.getInputStream(brand.getLogo_150_100());
															FileUtils.copyFile(is, path);
														};
														
													}.start();
													
													try {
														Thread.sleep(10);
													} catch (InterruptedException e) {
														// TODO Auto-generated catch block
														e.printStackTrace();
													}
													
													db.saveOrUpdate(brand);
													System.out
															.println(brand.getLocalizedName());
													
													try {
														Thread.sleep(10);
													} catch (InterruptedException e) {
														// TODO Auto-generated catch block
														e.printStackTrace();
													}
													
													//new Thread(){
													//	public void run() {
													initStores(cityId, brand.getId());
													//	};
													//}.start();
													
												} catch (DbException e) {
													// TODO Auto-generated catch block
													e.printStackTrace();
												}
											}
											handler.obtainMessage(1, "品牌完成").sendToTarget();
											
										}

										@Override
										public void onFailured(Result<Brand> result) {
											// TODO Auto-generated method stub
											
										}
										
									});
								}
								
							};
							
							@Override
							public void onFailured(Result<City> result) {
								// TODO Auto-generated method stub
								
							}
						});
						
					};
				}.start();
				
				handler.obtainMessage(1,"d").sendToTarget();
			}
		});
	};
	
	public void initCluster(final int cityId){
		context.cityId = String.valueOf(cityId);
		Client.getClusters(context, null, null, null, new ApiCallback<Cluster>(){
			@Override
			public void onCompleted(
					com.moyobar.app.api.ApiCallback.Result<Cluster> result) {
				try {
					if(result.getEntities() != null){
					for(Cluster cluster:result.getEntities()){
						City city = new City();
						city.setId(cityId);
						cluster.setCity(city);
						db.saveOrUpdate(cluster);
					}
					}
				} catch (DbException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			@Override
			public void onFailured(Result<Cluster> result) {
				// TODO Auto-generated method stub
				
			}
		});
	}
	
	public void initCategory(int cityId){
		context.cityId = String.valueOf(cityId);
		Client.getCategories(context, null, null, null, new ApiCallback<Category>(){
			
			@Override
			public void onCompleted(
					com.moyobar.app.api.ApiCallback.Result<Category> result) {
				try {
					for(final Category category:result.getEntities()){
						String fileName = category.getIcon_88_88().substring(8);
						fileName = fileName.substring(fileName.indexOf("/"));
						final String path = sdcard+appdir+fileName;
						
						category.setLocalIcon(fileName);
						
						new Thread(){
							
							public void run() {
								
								InputStream is = FileUtils.getInputStream(category.getIcon_88_88());
								FileUtils.copyFile(is, path);
							};
							
						}.start();
						
						db.saveOrUpdate(category);
					}
				} catch (DbException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			@Override
			public void onFailured(Result<Category> result) {
				// TODO Auto-generated method stub
				
			}
		});
	}
	
	@SuppressLint("NewApi")
	private void initStores(final int cityId,int brandId){
		System.out.println("初始化店铺");
		handler.obtainMessage(1, "品牌"+brandId+"的商店加载开始").sendToTarget();
		context.cityId = String.valueOf(cityId);
		Client.getStores(context, brandId, null, null, null, null, null, null, null, 0.5f, null, null, null, new ApiCallback<Store>(){

			@Override
			public void onCompleted(
					com.moyobar.app.api.ApiCallback.Result<Store> result) {
				List<Store> stores = result.getEntities();
				for(final Store store:stores){
					System.out.println(stores.size());
					try {
						store.setCityId(cityId);
						db.saveOrUpdate(store);
						try {
							Thread.sleep(10);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						handler.obtainMessage(1, store.getLocalizedTitle()+"商店信息").sendToTarget();
						Client.getStore(context, store.getId(), new ApiCallback<Store>(){

							@Override
							public void onCompleted(
									com.moyobar.app.api.ApiCallback.Result<Store> result) {
								try {
									
									final Store s = result.getEntity();
									System.out.println(s.getOpeningHours());
									if(s.getId() == 5133){
										System.out.println("222222222222222");
									}
									if(s.getStoreFront_180_120() != null && !s.getStoreFront_180_120().equals(StringUtils.EMPTY)){
										
										String fileName = s.getStoreFront_180_120().substring(8);
										fileName = fileName.substring(fileName.indexOf("/"));
										final String path = sdcard+appdir+fileName;
										
										s.setLocalStoreFront(fileName);
										
										new Thread(){
											
											public void run() {
												
												InputStream is = FileUtils.getInputStream(s.getStoreFront_180_120());
												FileUtils.copyFile(is, path);
											};
											
										}.start();
									}
									
									db.update(s, "title","openingHours","subUnit","localizedTitle","localizedOpeningHours","localizedSubUnit","email","unit","telephone","fax","type","localizedType","typeId","hasPromo","isFollow");
									
									db.saveOrUpdate(s.getBuilding());
									System.out.println(s.getBuilding().getAddress());
									System.out.println(s.getLocalizedTitle());

									try {
										Thread.sleep(10);
									} catch (InterruptedException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}
								} catch (DbException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							}

							@Override
							public void onFailured(Result<Store> result) {
								// TODO Auto-generated method stub
								
							}
							
						});
											
					} catch (DbException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}


			@Override
			public void onFailured(Result<Store> result) {
				// TODO Auto-generated method stub
				
			}
			
		});
	}
		
	class MainViewHandler extends Handler{
		
		@Override
		public void dispatchMessage(Message msg) {
			//super.dispatchMessage(msg);
			logView.setText(logView.getText()+"\n"+msg.obj);
			logView.setMovementMethod(ScrollingMovementMethod.getInstance());
			logView.setScrollbarFadingEnabled(false);
		}
		
	}
	
}
