/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package org.appcelerator.titanium.proxy;

import org.appcelerator.kroll.KrollProxy;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.kroll.common.AsyncResult;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.kroll.common.TiMessenger;
import org.appcelerator.titanium.TiApplication;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.util.TiActivityHelper.Command;
import org.appcelerator.titanium.util.TiActivityHelper.CommandNoReturn;
import org.appcelerator.titanium.util.TiConvert;
import org.appcelerator.titanium.util.TiFileHelper;
import org.appcelerator.titanium.util.TiUIHelper;
import org.appcelerator.titanium.util.TiUrl;
import org.appcelerator.titanium.view.TiCompositeLayout;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.os.Message;
import android.support.v4.view.MenuItemCompat;
import android.view.MenuItem;

@Kroll.proxy
public class MenuItemProxy extends AnimatableReusableProxy
{
	private static final String TAG = "MenuItem";

	private MenuItem item;


	private static final int MSG_FIRST_ID = KrollProxy.MSG_LAST_ID + 1;
	private static final int MSG_SET_ICON = MSG_FIRST_ID + 200;

	protected static final int MSG_LAST_ID = MSG_FIRST_ID + 1000;
	
	private final class CompatActionExpandListener implements MenuItemCompat.OnActionExpandListener {
        public boolean onMenuItemActionCollapse(MenuItem item) {
            fireEvent(TiC.EVENT_COLLAPSE, null);
            return true;
        }

        public boolean onMenuItemActionExpand(MenuItem item) {
            fireEvent(TiC.EVENT_EXPAND, null);
            return true;
        }
    }

    protected MenuItemProxy(MenuItem item, Activity activity)
	{
        super();
        setActivity(activity);
		this.item = item;
		MenuItemCompat.setOnActionExpandListener(item, new CompatActionExpandListener());
	}

	@Override
	public boolean handleMessage(Message msg) 
	{
		AsyncResult result = null;
		result = (AsyncResult) msg.obj;

		switch(msg.what) {
			case MSG_SET_ICON: {
				result.setResult(handleSetIcon(result.getArg()));
				return true;
			}		
			default : {
				return super.handleMessage(msg);
			}
		}
	}

	@Kroll.method @Kroll.getProperty
	public int getGroupId() {
	    return getValueInUIThread(new Command<Integer>() {
            public Integer execute() {
                return (Integer) item.getGroupId();
            };
        }, 0);
	}
	
	@Kroll.method @Kroll.getProperty
	public int getItemId() {
	    return getValueInUIThread(new Command<Integer>() {
            public Integer execute() {
                return (Integer) item.getItemId();
            };
        }, 0);
	}
	
	@Kroll.method @Kroll.getProperty
	public int getOrder() {
	    return getValueInUIThread(new Command<Integer>() {
            public Integer execute() {
                return (Integer) item.getOrder();
            };
        }, TiConvert.toInt(getProperty(TiC.PROPERTY_ORDER)));
	}
	
	@Kroll.method @Kroll.getProperty
	public String getTitle() {
	    return getValueInUIThread(new Command<String>() {
            public String execute() {
                return (String) item.getTitle();
            };
        },  TiConvert.toString(getProperty(TiC.PROPERTY_TITLE)));
	}

	
	@Kroll.method @Kroll.getProperty
	public String getTitleCondensed() {
	    return getValueInUIThread(new Command<String>() {
	        public String execute() {
	            return (String) item.getTitleCondensed();
	        };
        }, TiConvert.toString(getProperty(TiC.PROPERTY_TITLE_CONDENSED)));
	  
	}
	
	@Kroll.method
	public boolean hasSubMenu() {
	    return getValueInUIThread(new Command<Boolean>() {
            public Boolean execute() {
                return (Boolean) item.hasSubMenu();
            };
        }, false);
}
	
	@Kroll.method @Kroll.getProperty
	public boolean isChecked() {
	    return getValueInUIThread(new Command<Boolean>() {
            public Boolean execute() {
                return (Boolean) item.isChecked();
            };
        }, TiConvert.toBoolean(getProperty(TiC.PROPERTY_CHECKED)));
	}
	
	@Kroll.method @Kroll.getProperty
	public boolean isCheckable() {
	    return getValueInUIThread(new Command<Boolean>() {
            public Boolean execute() {
                return (Boolean) item.isChecked();
            };
        }, TiConvert.toBoolean(getProperty(TiC.PROPERTY_CHECKABLE)));
	}
	
	@Kroll.method @Kroll.getProperty
	public boolean isEnabled() {
	    return getValueInUIThread(new Command<Boolean>() {
            public Boolean execute() {
                return (Boolean) item.isEnabled();
            };
        }, TiConvert.toBoolean(getProperty(TiC.PROPERTY_ENABLED)));
	}
	
	@Kroll.method @Kroll.getProperty
	public boolean isVisible() {
	    return getValueInUIThread(new Command<Boolean>() {
            public Boolean execute() {
                return (Boolean) item.isVisible();
            };
        }, TiConvert.toBoolean(getProperty(TiC.PROPERTY_VISIBLE)));
	}
	

	private MenuItemProxy handleSetIcon(Object icon) 
	{
		if (icon != null) {
			if (icon instanceof String) {
				String iconPath = TiConvert.toString(icon);
				TiUrl iconUrl = new TiUrl(iconPath);
				if (iconPath != null) {
					TiFileHelper tfh = new TiFileHelper(TiApplication.getInstance());
					Drawable d = tfh.loadDrawable(iconUrl.resolve(), false);
					if (d != null) {
						item.setIcon(d);
					}
				}
			} else if (icon instanceof Number) {
				Drawable d = TiUIHelper.getResourceDrawable(TiConvert.toInt(icon));
				if (d != null) {
					item.setIcon(d);
				}
			}
		}
		return this;
	}
	public MenuItemProxy setIcon(Object icon) 
	{
		if (TiApplication.isUIThread()) {
			return handleSetIcon(icon);
		}

		return (MenuItemProxy) TiMessenger.sendBlockingMainMessage(getMainHandler().obtainMessage(MSG_SET_ICON), icon);	
	}

	public void setActionView(Object value)
	{
//        KrollProxy viewProxy = addProxyToHold(view, "rightButton");

		if (value instanceof TiViewProxy) {
		    
			final TiCompositeLayout layout = new TiCompositeLayout(getActivity());
            TiUIHelper.addView(layout, (TiViewProxy) value);            
			TiMessenger.postOnMain(new Runnable() {
				public void run() {
					if (TiC.ICS_OR_GREATER) {
	                    item.setActionView(layout);
	                } else {
	                    MenuItemCompat.setActionView(item, layout);
	                }
				}
			});
		} else {
			Log.w(TAG, "Invalid type for actionView", Log.DEBUG_MODE);
		}
	}



	@Kroll.method
	public void collapseActionView() {
	    runInUiThread(new CommandNoReturn() {
            public void execute() {
                if (TiC.ICS_OR_GREATER) {
                    item.collapseActionView();
                } else {
                    MenuItemCompat.collapseActionView(item);
                }
            }
        });
	}

	@Kroll.method
	public void expandActionView() {
	    runInUiThread(new CommandNoReturn() {
            public void execute() {
                if (TiC.ICS_OR_GREATER) {
                    item.expandActionView();
                } else {
                    MenuItemCompat.expandActionView(item);
                }
            }
        });
	}

	@Kroll.method @Kroll.getProperty
	public boolean isActionViewExpanded() {
	    return getValueInUIThread(new Command<Boolean>() {
            public Boolean execute() {
                if (TiC.ICS_OR_GREATER) {
                    return item.isActionViewExpanded();
                } else {
                    return MenuItemCompat.isActionViewExpanded(item);
                }
            };
        }, false);
	}

	@Override
	public String getApiName()
	{
		return "Ti.Android.MenuItem";
	}

	@Override
    public void propertySet(String key, Object newValue, Object oldValue,
            boolean changedProperty) {
        switch (key) {
        case TiC.PROPERTY_ACTION_VIEW:
            setActionView(newValue);
            break;
        case TiC.PROPERTY_CHECKABLE: 
        {
            final Boolean toApply = TiConvert.toBoolean(newValue);
            runInUiThread(new CommandNoReturn() {
                public void execute() {
                    item.setCheckable(toApply);
                }
            });
            break;
        }
        case TiC.PROPERTY_CHECKED:
        {
            final Boolean toApply = TiConvert.toBoolean(newValue);
            runInUiThread(new CommandNoReturn() {
                public void execute() {
                    item.setChecked(toApply);
                }
            });
            break;
        }
        case TiC.PROPERTY_ENABLED:
        {
            final Boolean toApply = TiConvert.toBoolean(newValue);
            runInUiThread(new CommandNoReturn() {
                public void execute() {
                    item.setEnabled(toApply);
                }
            });
            break;
        }
        case TiC.PROPERTY_ICON:
            setIcon(newValue);
            break;
        case TiC.PROPERTY_SHOW_AS_ACTION:
        {
            final int toApply = TiConvert.toInt(newValue);
            runInUiThread(new CommandNoReturn() {
                public void execute() {
                    if (TiC.ICS_OR_GREATER) {
                        item.setShowAsAction(toApply);
                    } else {
                        MenuItemCompat.setShowAsAction(item, toApply);
                    }
                }
            });
            break;
        }
        case TiC.PROPERTY_TITLE_CONDENSED:
        {
            final String toApply = TiConvert.toString(newValue);
            runInUiThread(new CommandNoReturn() {
                public void execute() {
                    item.setTitleCondensed(toApply);
                }
            });
            break;
        }
        case TiC.PROPERTY_TITLE:
        {
            final String toApply = TiConvert.toString(newValue);
            runInUiThread(new CommandNoReturn() {
                public void execute() {
                    item.setTitle(toApply);
                }
            });
            break;
        }
        case TiC.PROPERTY_VISIBLE:
        {
            final Boolean toApply = TiConvert.toBoolean(newValue);
            runInUiThread(new CommandNoReturn() {
                public void execute() {
                    item.setVisible(toApply);
                }
            });
            break;
        }
        default:
            super.propertySet(key, newValue, oldValue, changedProperty);
            break;
        }
    }


}
