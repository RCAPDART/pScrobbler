package com.arn.scrobble

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.res.Configuration
import android.os.Bundle
import android.transition.Slide
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.GridLayoutManager
import com.arn.scrobble.ui.FocusChangeListener
import com.arn.scrobble.ui.ItemClickListener
import de.umass.lastfm.ImageSize
import de.umass.lastfm.Track
import kotlinx.android.synthetic.main.content_similar.*
import kotlinx.android.synthetic.main.content_similar.view.*
import kotlinx.android.synthetic.main.coordinator_main.*
import kotlinx.android.synthetic.main.coordinator_main.view.*
import kotlinx.android.synthetic.main.header_default.view.*
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Created by arn on 29/12/2017.
 */
class SimilarTracksFragment : Fragment(), ItemClickListener, FocusChangeListener {
    private lateinit var similarLd: MutableLiveData<List<Track>>
    private lateinit var viewModel:TracksVM
    private lateinit var adapter: SimilarTracksAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = Slide(Gravity.TOP)
        returnTransition = Slide(Gravity.TOP)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.content_similar, container, false)
        view.isNestedScrollingEnabled = false

        val glm = GridLayoutManager(context!!, 1)
        adapter = SimilarTracksAdapter(view)

        calcGridSize(glm)

        view.similar_grid.layoutManager = glm
        adapter.setClickListener(this)
        adapter.setFocusListener(this)
        view.similar_grid.adapter = adapter
        view.header_text.text = getString(R.string.similar_tracks)

        val activity = activity!!
        val similarAnimator = ObjectAnimator.ofFloat(activity.hero_similar, View.Y,
                activity.hero_similar.y, activity.hero_similar.y + Stuff.dp2px(50, activity).toFloat())
        val graphAnimator = ObjectAnimator.ofFloat(activity.sparkline_frame, View.Y,
                activity.sparkline_frame.y, activity.sparkline_frame.y + Stuff.dp2px(150, activity).toFloat())
        val animSet = AnimatorSet()
        animSet.playTogether(similarAnimator, graphAnimator)
        animSet.interpolator = AccelerateInterpolator()
        animSet.duration = 400
        animSet.addListener(
                object : Animator.AnimatorListener {
                    override fun onAnimationRepeat(p0: Animator?) {
                    }

                    override fun onAnimationCancel(p0: Animator?) {
                    }

                    override fun onAnimationStart(p0: Animator?) {
                    }

                    override fun onAnimationEnd(p0: Animator?) {
                        val activity = getActivity() ?: return
                        Stuff.setAppBarHeight(activity, adapter.itemSizeDp*3)
                        activity.ctl.hero_similar.isEnabled = true
                        activity.ctl.hero_similar.visibility = View.GONE
                        activity.ctl.sparkline_frame.visibility = View.GONE
                        activity.ctl.hero_title.postDelayed({
                            activity.ctl.hero_title.visibility = View.VISIBLE
                        }, 50)
                    }

                }
        )
        animSet.start()

        similarLd.observe(viewLifecycleOwner, Observer {
            it ?: return@Observer
            if (Main.isOnline)
                similar_linear_layout?.header_text?.text = getString(R.string.similar_tracks)
            else
                similar_linear_layout?.header_text?.text = getString(R.string.offline)
            adapter.populate(it)

            it.forEachIndexed { i, t ->
                viewModel.loadInfo(t.artist, t.name, i)
            }
        })

        viewModel.trackInfo.observe(viewLifecycleOwner, Observer {
            it ?: return@Observer
            val img = it.second.getWebpImageURL(ImageSize.EXTRALARGE)
            if(img != null)
                adapter.setImg(it.first, img)
        })
        return view
    }

    override fun onDestroyView() {
        val activity = activity!!
        if (activity.ctl.hero_similar.isInTouchMode)
            activity.ctl.hero_similar.visibility = View.VISIBLE
        else
            activity.ctl.hero_similar.visibility = View.INVISIBLE
        activity.ctl.hero_title.visibility = View.GONE
        activity.ctl.sparkline_frame.visibility = View.VISIBLE
        Stuff.setAppBarHeight(activity)

        val similarAnimator = ObjectAnimator.ofFloat(activity.hero_similar, View.Y,
                activity.hero_similar.y, activity.hero_similar.y - Stuff.dp2px(50, activity).toFloat())
        val graphAnimator = ObjectAnimator.ofFloat(activity.sparkline_frame, View.Y,
                activity.sparkline_frame.y, activity.sparkline_frame.y - Stuff.dp2px(150, activity).toFloat())
        val animSet = AnimatorSet()
        animSet.playTogether(similarAnimator, graphAnimator)
        animSet.interpolator = DecelerateInterpolator()
        animSet.duration = 400
        animSet.start()

        super.onDestroyView()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        if (isVisible)
            Stuff.setAppBarHeight(activity!!, adapter.itemSizeDp*3)
        val glm = similar_grid?.layoutManager as GridLayoutManager? ?: return
        calcGridSize(glm)
//            adapter.notifyDataSetChanged()
    }

    private fun calcGridSize(glm: GridLayoutManager) {
        val cols: Int
        var w = resources.getDimension(R.dimen.grid_size).roundToInt()
        val screenW = resources.displayMetrics.widthPixels
        cols = screenW / w
        w += (screenW % w)/cols
        var rows = max(resources.displayMetrics.heightPixels/3/w, 3)
        if (rows * cols <= 6)
            rows++

        adapter.itemSizeDp = w/resources.displayMetrics.densityDpi
        glm.spanCount = cols
        viewModel = VMFactory.getVM(this, TracksVM::class.java)
        val arguments = arguments!!
        similarLd = viewModel.loadSimilar(arguments.getString("artist")!!, arguments.getString("track")!!, rows * cols)
    }

    override fun onItemClick (view: View, position: Int) {
        val adapter = similar_grid.adapter as SimilarTracksAdapter
        val track = adapter.getItem(position)
        Stuff.launchSearchIntent(track.artist, track.name, context!!)
    }

    //only called when !view.isInTouchMode
    override fun onFocus(view: View, position: Int) {
        val pos = IntArray(2)
        view.getLocationInWindow(pos)

        if (pos[1] + view.height > activity!!.coordinator.height && activity!!.app_bar.isExpanded)
            activity!!.app_bar.setExpanded(false, true)
    }
}