/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package org.appcelerator.titanium.proxy;

import org.appcelerator.kroll.KrollProxy;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.kroll.common.TiMessenger;
import org.appcelerator.kroll.common.TiMessenger.Command;
import org.appcelerator.kroll.common.TiMessenger.CommandNoReturn;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.util.TiConvert;
import org.appcelerator.titanium.util.TiUIHelper;
import org.appcelerator.titanium.view.TiCompositeLayout;

import android.app.Activity;
import android.support.v4.view.MenuItemCompat;
import android.view.MenuItem;

@Kroll.proxy
public class MenuItemProxy extends AnimatableReusableProxy
{
	private static final String TAG = "MenuItem";

	private MenuItem item;


	private static final int MSG_FIRST_ID = KrollProxy.MSG_LAST_ID + 1;

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
        }, false);
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
        }, false);
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
            }, false);
            break;
        }
        case TiC.PROPERTY_CHECKED:
        {
            final Boolean toApply = TiConvert.toBoolean(newValue);
            runInUiThread(new CommandNoReturn() {
                public void execute() {
                    item.setChecked(toApply);
                }
            }, false);
            break;
        }
        case TiC.PROPERTY_ENABLED:
        {
            final Boolean toApply = TiConvert.toBoolean(newValue);
            runInUiThread(new CommandNoReturn() {
                public void execute() {
                    item.setEnabled(toApply);
                }
            }, false);
            break;
        }
        case TiC.PROPERTY_ICON:
        {
            final Object toApply = newValue;
            runInUiThread(new CommandNoReturn() {
                public void execute() {
                    item.setIcon(TiUIHelper.getResourceDrawable(toApply));
                }
            }, false);
            break;
        }
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
            }, false);
            break;
        }
        case TiC.PROPERTY_TITLE_CONDENSED:
        {
            final String toApply = TiConvert.toString(newValue);
            runInUiThread(new CommandNoReturn() {
                public void execute() {
                    item.setTitleCondensed(toApply);
                }
            }, false);
            break;
        }
        case TiC.PROPERTY_TITLE:
        {
            final String toApply = TiConvert.toString(newValue);
            runInUiThread(new CommandNoReturn() {
                public void execute() {
                    item.setTitle(toApply);
                }
            }, false);
            break;
        }
        case TiC.PROPERTY_VISIBLE:
        {
            final Boolean toApply = TiConvert.toBoolean(newValue);
            runInUiThread(new CommandNoReturn() {
                public void execute() {
                    item.setVisible(toApply);
                }
            }, false);
            break;
        }
        default:
            super.propertySet(key, newValue, oldValue, changedProperty);
            break;
        }
    }
}
