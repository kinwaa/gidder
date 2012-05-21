package net.antoniy.gidder.ui.activity;

import java.sql.SQLException;

import net.antoniy.gidder.R;
import net.antoniy.gidder.db.entity.Repository;
import net.antoniy.gidder.git.GitRepositoryDao;
import net.antoniy.gidder.ui.util.C;
import net.antoniy.gidder.ui.util.GidderCommons;

import org.eclipse.jgit.errors.RepositoryNotFoundException;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.Toast;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

public class AddRepositoryActivity extends BaseActivity {
	private final static String TAG = AddRepositoryActivity.class.getSimpleName();
	
	public final static int REQUEST_CODE_ADD_REPOSITORY = 1;
	public final static int REQUEST_CODE_EDIT_REPOSITORY = 2;
	
//	private Button addEditButton;
//	private Button cancelButton;
	private EditText nameEditText;
	private EditText mappingEditText;
	private EditText descriptionEditText;
	private boolean editMode = false;
	private int repositoryId;
	private GitRepositoryDao repositoryDao;

	@Override
	protected void setup() {
		setContentView(R.layout.add_repository);
		
		if(getIntent().getExtras() != null) {
			repositoryId = getIntent().getExtras().getInt("repositoryId", -1);
			Log.i(TAG, "RepositoryID: " + repositoryId);
			
			if(repositoryId > 0) {
				editMode = true;
			} else {
				editMode = false;
			}
		}
		
		this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
	}

	@Override
	protected void initComponents(Bundle savedInstanceState) {
		repositoryDao = new GitRepositoryDao(this);
		
//		addEditButton = (Button) findViewById(R.id.addRepositoryBtnAdd);
//		addEditButton.setOnClickListener(this);
//		if(editMode) {
//			addEditButton.setText(R.string.btn_save);
//		} else {
//			addEditButton.setText(R.string.btn_add);
//		}
//		
//		cancelButton = (Button) findViewById(R.id.addRepositoryBtnCancel);
//		cancelButton.setOnClickListener(this);
		
		nameEditText = (EditText) findViewById(R.id.addRepositoryName);
		nameEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if(!hasFocus && nameEditText.getText().length() > 0 && mappingEditText.getText().length() == 0) {
					mappingEditText.setText(GidderCommons.toCamelCase(nameEditText.getText().toString()));
				}
			}
		});
		
		mappingEditText = (EditText) findViewById(R.id.addRepositoryMapping);
		descriptionEditText = (EditText) findViewById(R.id.addRepositoryDescription);
		
		if(editMode) {
			populateFieldsWithRepositoryData();
		}
	}
	
	@Override
	protected void setupActionBar() {
		if(editMode) {
        	getSupportActionBar().setTitle(R.string.add_repository_edittitle);
        } else {
        	getSupportActionBar().setTitle(R.string.add_repository_title);
        }
		
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if(item.getItemId() == android.R.id.home) {
			Intent intent = new Intent(C.action.START_SETUP_ACTIVITY);
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			
			finish();
			startActivity(intent);
		}
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuItem doneMenuItem = menu.add("Done").setIcon(R.drawable.ic_actionbar_accept);
		doneMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
		doneMenuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
			
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				processRepositoryAction();
				return true;
			}
			
		});
		
		MenuItem cancelMenuItem = menu.add("Cancel").setIcon(R.drawable.ic_actionbar_cancel);
		cancelMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
		cancelMenuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
			
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				finish();
				return false;
			}
			
		});
		
		return super.onCreateOptionsMenu(menu);
	}
	
	private void populateFieldsWithRepositoryData() {
		Repository repository = null;
		try {
			repository = getHelper().getRepositoryDao().queryForId(repositoryId);
		} catch (SQLException e) {
			Log.e(TAG, "Error retrieving repository with id " + repositoryId, e);
			return;
		}
		
		nameEditText.setText(repository.getName());
		mappingEditText.setText(repository.getMapping());
		descriptionEditText.setText(repository.getDescription());
	}

//	@Override
//	public void onClick(View v) {
//		super.onClick(v);
//		
//		if(v.getId() == R.id.addRepositoryBtnAdd) {
//			processRepositoryAction();
//		} else if(v.getId() == R.id.addRepositoryBtnCancel) {
//			finish();
//		}
//	}
	
	private void processRepositoryAction() {
		if(!isFieldsValid()) {
			return;
		}
		
		final String name = nameEditText.getText().toString();
		final String mapping = mappingEditText.getText().toString();
		final String description = descriptionEditText.getText().toString();
		
		if(editMode) {
			final ProgressDialog dialog = ProgressDialog.show(AddRepositoryActivity.this, "", "Renaming repository...", true);
			dialog.show();
			
			new Thread(new Runnable() {
				public void run() {
					Looper.prepare();
					
					try {
						repositoryDao.renameRepository(repositoryId, mapping);
			
						// TODO: Fix edit of active and create datetime.
						getHelper().getRepositoryDao().update(new Repository(repositoryId, name, mapping, description, true, System.currentTimeMillis()));
						
						setResult(RESULT_OK, null);
						finish();
					} catch (SQLException e) {
						Log.e(TAG, "Problem when add new repository.", e);
						Toast.makeText(AddRepositoryActivity.this, "Error! Database error.", Toast.LENGTH_SHORT).show();
					} finally {
						dialog.dismiss();
						Looper.loop();
					}
				}
			}).start();
		} else {
			final ProgressDialog dialog = ProgressDialog.show(AddRepositoryActivity.this, "", "Creating repository...", true);
			dialog.show();

			new Thread(new Runnable() {
				public void run() {
					Looper.prepare();
					
					try {
						getHelper().getRepositoryDao().create(new Repository(0, name, mapping, description, true, System.currentTimeMillis()));
						repositoryDao.createRepository(mapping);

						setResult(RESULT_OK, null);
						finish();
					} catch (RepositoryNotFoundException e) {
						Log.e(TAG, "Problem while creating repository.", e);
						Toast.makeText(AddRepositoryActivity.this, "Error! Cannot create repository.", Toast.LENGTH_SHORT).show();
					} catch (SQLException e) {
						Log.e(TAG, "Problem when add new repository.", e);
						Toast.makeText(AddRepositoryActivity.this, "Error! Database error.", Toast.LENGTH_SHORT).show();
					} finally {
						dialog.dismiss();
						Looper.loop();
					}
				}
			}).start();
		}
	}
	
	private boolean isFieldsValid() {
		boolean isAllFieldsValid = true;
		
		if(!isNameValid()) {
			isAllFieldsValid = false;
		}
		
		if(!isMappingValid()) {
			isAllFieldsValid = false;
		}
		
		return isAllFieldsValid;
	}
	
	private boolean isEditTextEmpty(EditText tv) {
		String text = tv.getText().toString();
		if("".equals(text.trim())) {
			tv.startAnimation(AnimationUtils.loadAnimation(AddRepositoryActivity.this, R.anim.shake));
			tv.setError("Field must contain value");
			return true;
		} else {
			return false;
		}
	}
	
	private boolean isNameValid() {
		return !isEditTextEmpty(nameEditText);
	}
	
	private boolean isMappingValid() {
		return !isEditTextEmpty(mappingEditText);
	}
	
}
