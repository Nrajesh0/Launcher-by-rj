package dev.dothome.ui

import android.app.AlertDialog
import android.content.ContentValues
import android.graphics.*
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.view.*
import android.widget.*
import app.lawnchair.preferences2.PreferenceManager2
import com.android.launcher3.*
import com.android.launcher3.model.data.*
import com.android.launcher3.statemanager.StateManager
import com.android.launcher3.util.ContentWriter
import com.patrykmichalik.opto.core.firstBlocking
import java.util.WeakHashMap

/** Selection operates on native workspace items; writes complete in one DB transaction. */
object DotArrange {
    private val sessions=WeakHashMap<Launcher,Session>()
    @JvmStatic fun locked(launcher: Launcher)=PreferenceManager2.getInstance(launcher).lockHomeScreen.firstBlocking()
    @JvmStatic fun begin(launcher: Launcher) {
        if(locked(launcher))return
        if(sessions.containsKey(launcher))return
        val session=Session(launcher);sessions[launcher]=session;session.open()
    }
    @JvmStatic fun end(launcher: Launcher) { sessions[launcher]?.close() }
    @JvmStatic fun select(view: View): Boolean {
        val launcher=Launcher.getLauncher(view.context)
        val session=sessions[launcher] ?: return false
        val item=view.tag as? ItemInfo ?: return false
        if(item.container!=LauncherSettings.Favorites.CONTAINER_DESKTOP || (item !is WorkspaceItemInfo && item !is FolderInfo && item !is LauncherAppWidgetInfo))return false
        session.toggle(view,item);return true
    }
    @JvmStatic fun touch(launcher: Launcher,event: MotionEvent): Boolean = sessions[launcher]?.touch(event) ?: false
    private class Session(val launcher: Launcher): StateManager.StateListener<LauncherState> {
        val selected=linkedMapOf<Int,Pair<View,ItemInfo>>()
        val badges=mutableMapOf<Int,Drawable>()
        val bar=LinearLayout(launcher)
        val count=TextView(launcher)
        val actions=mutableListOf<TextView>()
        var touched: View?=null
        var downX=0f;var downY=0f;var downTime=0L
        var dragX=0f;var dragY=0f;var dragging=false
        var ghost: View?=null
        val handler=android.os.Handler(android.os.Looper.getMainLooper())
        val edge=object: Runnable {override fun run() {
            if(!dragging)return
            val workspace=launcher.workspace
            val direction=if(dragX<d(40))-1 else if(dragX>launcher.dragLayer.width-d(40))1 else 0
            if(direction!=0)workspace.snapToPage((workspace.currentPage+direction).coerceIn(0,workspace.pageCount-1))
            handler.postDelayed(this,800)
        }}
        fun touch(event: MotionEvent): Boolean {
            when(event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    touched=null
                    val page=launcher.workspace.getScreenWithId(launcher.workspace.getScreenIdForPageIndex(launcher.workspace.currentPage)) ?: return false
                    val bounds=Rect();val children=page.shortcutsAndWidgets
                    for(n in 0 until children.childCount) {
                        val view=children.getChildAt(n);val item=view.tag as? ItemInfo ?: continue
                        if(item.container==LauncherSettings.Favorites.CONTAINER_DESKTOP && view.getGlobalVisibleRect(bounds) && bounds.contains(event.rawX.toInt(),event.rawY.toInt())) {touched=view;break}
                    }
                    downX=event.x;downY=event.y;downTime=event.eventTime
                }
                MotionEvent.ACTION_MOVE -> if(touched!=null) {
                    dragX=event.x;dragY=event.y
                    if(!dragging && event.eventTime-downTime>=250 && kotlin.math.hypot(event.x-downX,event.y-downY)>d(12)) {
                        val view=touched!!;val item=view.tag as ItemInfo
                        if(item.id !in selected)toggle(view,item)
                        dragging=true
                        ghost=object: View(launcher) {
                            val paint=Paint(Paint.ANTI_ALIAS_FLAG)
                            override fun onDraw(canvas: Canvas) {
                                paint.color=0xee232523.toInt();canvas.drawRoundRect(0f,0f,width.toFloat(),height.toFloat(),d(22).toFloat(),d(22).toFloat(),paint)
                                paint.color=Color.WHITE;paint.textSize=d(18).toFloat();paint.textAlign=Paint.Align.CENTER
                                canvas.drawText("${selected.size} items",width/2f,height/2f+d(6),paint)
                            }
                        }.also {launcher.dragLayer.addView(it,com.android.launcher3.views.BaseDragLayer.LayoutParams(d(112),d(72)))}
                        handler.postDelayed(edge,800)
                        touched?.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    }
                    ghost?.let {it.translationX=event.x-it.width/2;it.translationY=event.y-it.height/2}
                }
                MotionEvent.ACTION_UP -> if(touched!=null) {
                    if(dragging)drop(event.rawX,event.rawY) else touched?.let {select(it)}
                    clearDrag();return true
                }
                MotionEvent.ACTION_CANCEL -> {val captured=touched!=null;clearDrag();return captured}
            }
            return touched!=null || event.actionMasked==MotionEvent.ACTION_UP && (dragging || ghost!=null)
        }
        fun clearDrag() {handler.removeCallbacks(edge);ghost?.let {(it.parent as? ViewGroup)?.removeView(it)};ghost=null;dragging=false;touched=null}
        fun drop(rawX: Float,rawY: Float) {
            val workspace=launcher.workspace;val target=workspace.getScreenIdForPageIndex(workspace.currentPage)
            val page=workspace.getScreenWithId(target) ?: return
            val bounds=Rect();page.getGlobalVisibleRect(bounds)
            if(!bounds.contains(rawX.toInt(),rawY.toInt()))return
            val cell=IntArray(2)
            page.pointToCellExact(((rawX-bounds.left)*page.width/bounds.width()).toInt(),((rawY-bounds.top)*page.height/bounds.height()).toInt(),cell)
            val items=selected.values.map {it.second};if(items.isEmpty())return
            val minX=items.minOf {it.cellX};val minY=items.minOf {it.cellY}
            val grid=InvariantDeviceProfile.INSTANCE.get(launcher)
            val positions=items.map {Rect(cell[0]+it.cellX-minX,cell[1]+it.cellY-minY,cell[0]+it.cellX-minX+it.spanX,cell[1]+it.cellY-minY+it.spanY)}
            val occupied=mutableListOf<Rect>();val children=page.shortcutsAndWidgets
            for(n in 0 until children.childCount) {val other=children.getChildAt(n).tag as? ItemInfo ?: continue;if(other.id !in selected)occupied.add(Rect(other.cellX,other.cellY,other.cellX+other.spanX,other.cellY+other.spanY))}
            if(positions.any {it.left<0||it.top<0||it.right>grid.numColumns||it.bottom>grid.numRows||occupied.any {other->Rect.intersects(it,other)}} || positions.indices.any {a->positions.indices.any {b->a<b&&Rect.intersects(positions[a],positions[b])}}) {Toast.makeText(launcher,"Make room for this selection",Toast.LENGTH_SHORT).show();return}
            transaction {db->val committedScreen=if(target<0)db.getNewScreenId() else target;items.forEachIndexed {n,item->val values=ContentValues();values.put("screen",committedScreen);values.put("cellX",positions[n].left);values.put("cellY",positions[n].top);check(db.update("favorites",values,"_id=?",arrayOf(item.id.toString()))==1)}}
        }
        fun d(value: Int)=(value*launcher.resources.displayMetrics.density).toInt()
        fun open() {
            val dark=launcher.getSharedPreferences("dot-style",0).getBoolean("dark",false)
            bar.gravity=Gravity.CENTER_VERTICAL;bar.setPadding(d(12),d(4),d(8),d(4))
            bar.background=GradientDrawable().apply {setColor(if(dark)0xff252625.toInt() else 0xfff8f8f3.toInt());cornerRadius=d(28).toFloat()}
            count.textSize=13f;count.setTextColor(if(dark)Color.WHITE else Color.BLACK)
            bar.addView(count,LinearLayout.LayoutParams(0,-1,1f))
            listOf("Move","Style","Folder","Remove","Done").forEachIndexed { index,label ->
                val button=TextView(launcher).apply {text=label;textSize=12f;gravity=Gravity.CENTER;setTextColor(if(dark)Color.WHITE else Color.BLACK);setOnClickListener {when(index){0->move();1->style();2->folder();3->remove();else->close()}}}
                bar.addView(button,LinearLayout.LayoutParams(d(47),d(48)));actions.add(button)
            }
            launcher.dragLayer.addView(bar,com.android.launcher3.views.BaseDragLayer.LayoutParams(-1,d(58)).apply {gravity=Gravity.BOTTOM;leftMargin=d(14);rightMargin=d(14);bottomMargin=launcher.deviceProfile.insets.bottom+d(90)})
            launcher.stateManager.addStateListener(this)
            launcher.stateManager.goToState(LauncherState.EDIT_MODE)
            update()
        }
        fun update() {count.text=if(selected.isEmpty())"Select items" else "${selected.size} selected";actions.take(4).forEach {it.isEnabled=selected.isNotEmpty();it.alpha=if(it.isEnabled)1f else .4f}}
        fun toggle(view: View,item: ItemInfo) {
            if(selected.remove(item.id)!=null) {badges.remove(item.id)?.let {view.overlay.remove(it)}} else {
                selected[item.id]=view to item
                val badge=object: Drawable() {
                    override fun draw(canvas: Canvas) {val paint=Paint(Paint.ANTI_ALIAS_FLAG);paint.color=0xffd71920.toInt();val radius=d(10).toFloat();val x=view.width-radius;val y=radius;canvas.drawCircle(x,y,radius,paint);paint.color=Color.WHITE;paint.style=Paint.Style.STROKE;paint.strokeWidth=d(2).toFloat();val tick=Path();tick.moveTo(x-radius*.5f,y);tick.lineTo(x-radius*.15f,y+radius*.35f);tick.lineTo(x+radius*.5f,y-radius*.4f);canvas.drawPath(tick,paint)}
                    override fun setAlpha(value: Int) {};override fun setColorFilter(filter: ColorFilter?) {};override fun getOpacity()=PixelFormat.TRANSLUCENT
                }
                badge.setBounds(0,0,view.width,view.height);view.overlay.add(badge);badges[item.id]=badge
            };update()
        }
        fun close() {
            clearDrag()
            selected.forEach {(id,pair)->badges[id]?.let {pair.first.overlay.remove(it)}}
            selected.clear();badges.clear();(bar.parent as? android.view.ViewGroup)?.removeView(bar)
            launcher.stateManager.removeStateListener(this);sessions.remove(launcher)
            if(launcher.isInState(LauncherState.EDIT_MODE))launcher.stateManager.goToState(LauncherState.NORMAL)
        }
        override fun onStateTransitionStart(toState: LauncherState) {if(toState!=LauncherState.EDIT_MODE)close()}
        fun transaction(write: (com.android.launcher3.model.ModelDbController)->Unit) {
            val app=LauncherAppState.getInstance(launcher)
            app.model.enqueueModelUpdateTask {_,_,_->
                try {
                    val db=app.model.modelDbController
                    db.newTransaction().use {tx->write(db);tx.commit()}
                    launcher.runOnUiThread {close()};app.model.forceReload()
                } catch(error: Exception) {launcher.runOnUiThread {Toast.makeText(launcher,"Items kept in place; arrangement could not finish",Toast.LENGTH_LONG).show()}}
            }
        }
        fun move() {
            val order=launcher.workspace.screenOrder.toArray().filter {it>=0}
            val labels=order.mapIndexed {index,_->"Page ${index+1}"}+"New page"
            AlertDialog.Builder(launcher).setTitle("Move selection").setItems(labels.toTypedArray()) {_,index->
                val chosen=selected.values.map {it.second}.toList()
                val minX=chosen.minOf {it.cellX};val minY=chosen.minOf {it.cellY}
                val grid=InvariantDeviceProfile.INSTANCE.get(launcher)
                var target=if(index<order.size)order[index] else -1
                val occupied=mutableListOf<Rect>()
                launcher.workspace.getScreenWithId(target)?.shortcutsAndWidgets?.let {children->
                    for(n in 0 until children.childCount) {val other=children.getChildAt(n).tag as? ItemInfo ?: continue;if(other.id !in selected)occupied.add(Rect(other.cellX,other.cellY,other.cellX+other.spanX,other.cellY+other.spanY))}
                }
                var cells: List<Rect>?=null
                search@ for(y in 0 until grid.numRows)for(x in 0 until grid.numColumns) {
                    val candidate=chosen.map {Rect(x+it.cellX-minX,y+it.cellY-minY,x+it.cellX-minX+it.spanX,y+it.cellY-minY+it.spanY)}
                    if(candidate.any {it.left<0||it.top<0||it.right>grid.numColumns||it.bottom>grid.numRows||occupied.any {other->Rect.intersects(it,other)}})continue
                    if(candidate.indices.any {a->candidate.indices.any {b->a<b&&Rect.intersects(candidate[a],candidate[b])}})continue
                    cells=candidate;break@search
                }
                val placement=cells
                if(placement==null) {Toast.makeText(launcher,"The selection needs more space on this page",Toast.LENGTH_SHORT).show();return@setItems}
                transaction {db->
                    if(target<0)target=db.getNewScreenId()
                    chosen.forEachIndexed {n,item->val cell=placement[n];val values=ContentValues();values.put("screen",target);values.put("cellX",cell.left);values.put("cellY",cell.top);check(db.update("favorites",values,"_id=?",arrayOf(item.id.toString()))==1)}
                }
            }.setNegativeButton("Cancel",null).show()
        }
        fun style() {
            val item=selected.values.singleOrNull()?.second
            if(item is FolderInfo) {DotFolderPreview.choose(launcher,item,selected.values.single().first);return}
            if(item is LauncherAppWidgetInfo && dev.dothome.widgets.WidgetEditor.open(launcher,item)) {close();return}
            Toast.makeText(launcher,"Select one widget or folder to edit its style",Toast.LENGTH_SHORT).show()
        }
        fun folder() {
            val items=selected.values.map {it.second}
            if(items.size<2 || items.any {it !is WorkspaceItemInfo}) {Toast.makeText(launcher,"Select at least two apps",Toast.LENGTH_SHORT).show();return}
            val name=EditText(launcher).apply {setText("Folder");selectAll()}
            AlertDialog.Builder(launcher).setTitle("Folder name").setView(name).setPositiveButton("Create") {_,_->
                transaction {db->
                    val first=items.first();val folder=FolderInfo();folder.id=db.generateNewItemId();folder.title=name.text.toString().ifBlank {"Folder"};folder.container=LauncherSettings.Favorites.CONTAINER_DESKTOP;folder.screenId=first.screenId;folder.cellX=first.cellX;folder.cellY=first.cellY
                    val writer=ContentWriter(launcher);folder.onAddToDatabase(writer);val row=writer.getValues(launcher);row.put("_id",folder.id);check(db.insert("favorites",row)>=0)
                    items.forEachIndexed {rank,item->val values=ContentValues();values.put("container",folder.id);values.put("screen",0);values.put("rank",rank);values.put("cellX",rank%3);values.put("cellY",rank/3);values.put("spanX",1);values.put("spanY",1);check(db.update("favorites",values,"_id=?",arrayOf(item.id.toString()))==1)}
                }
            }.setNegativeButton("Cancel",null).show()
        }
        fun remove() {
            val ids=selected.keys.toList()
            AlertDialog.Builder(launcher).setTitle("Remove ${ids.size} items from Home?").setMessage("Your apps stay installed.").setPositiveButton("Remove") {_,_->transaction {db->ids.forEach {id->db.delete("favorites","_id=? OR container=?",arrayOf(id.toString(),id.toString()))}}}.setNegativeButton("Cancel",null).show()
        }
    }
}
