package com.arn.ytscrobble;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.Toolbar;
import android.text.format.DateUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import de.umass.lastfm.ImageSize;
import de.umass.lastfm.PaginatedResult;
import de.umass.lastfm.Track;

import static android.text.format.DateUtils.MINUTE_IN_MILLIS;

/**
 * Created by arn on 10/07/2017.
 */

class RecentsAdapter extends ArrayAdapter<Track> {

    private final ImageView hero;
    private int layoutResourceId;
        private static final Integer FILLED = 5;
//        private ArrayList<Track> tracks;

        RecentsAdapter(Context c, int layoutResourceId) {
            super(c, layoutResourceId, new ArrayList<Track>());
            this.layoutResourceId = layoutResourceId;
            hero =  (ImageView)((Activity)c).findViewById(R.id.img_hero);
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

        /*
         * The convertView argument is essentially a "ScrapView" as described is Lucas post
         * http://lucasr.org/2012/04/05/performance-tips-for-androids-listview/
         * It will have a non-null value when ListView is asking you recycle the row layout.
         * So, when convertView is not null, you should simply update its contents instead of inflating a new row layout.
         */
            if(convertView==null){
                // inflate the layout
                LayoutInflater inflater = ((Activity) getContext()).getLayoutInflater();
                convertView = inflater.inflate(layoutResourceId, parent, false);
            }

            // object item based on the position
            final Track t = getItem(position);

            // get the TextView and then set the text (item name) and tag (item ID) values
            TextView title = (TextView) convertView.findViewById(R.id.recents_title),
                    subtitle = (TextView) convertView.findViewById(R.id.recents_subtitle),
                    date = (TextView) convertView.findViewById(R.id.recents_date);
            String np = t.isNowPlaying() ? "▶️" : "";
            CharSequence relDate = t.isNowPlaying() ? "playing right now..." : "";
            title.setText(np + t.getName());
            subtitle.setText(t.getArtist());

            if (t.getPlayedWhen() != null) {
                relDate = DateUtils.getRelativeTimeSpanString(
                        t.getPlayedWhen().getTime(), System.currentTimeMillis(), MINUTE_IN_MILLIS);
                if (relDate.charAt(0) == '0')
                    relDate = "Just now";
            }
            date.setText(relDate);
            final ImageButton love = (ImageButton) convertView.findViewById(R.id.recents_love);
            love.setOnClickListener(loveToggle);

            if (t.isLoved()) {
                love.setImageResource(R.drawable.ic_line_heart_enabled);
                love.setTag(R.id.recents_love, FILLED);
            } else {
                love.setImageResource(R.drawable.ic_line_heart_disabled);
                love.setTag(R.id.recents_love, 0);
            }

            ImageView albumArt =  (ImageView)convertView.findViewById(R.id.recents_album_art);

            String imgUrl = t.getImageURL(ImageSize.MEDIUM);

            if (imgUrl != null && !imgUrl.equals("")) {
                albumArt.clearColorFilter();
                Picasso.with(getContext())
                        .load(imgUrl)
                        .fit()
                        .centerInside()
                        .placeholder(R.drawable.ic_lastfm)
                        .error(R.drawable.ic_placeholder_music)
                        .into(albumArt);

            } else {
                albumArt.setImageResource(R.drawable.ic_placeholder_music);
                albumArt.setColorFilter(Stuff.getMatColor(getContext(),"500"));
            }

            if (position == 0 && !t.getUrl().equals(hero.getTag())){
                FloatingActionButton fab = (FloatingActionButton) ((Activity)getContext()).findViewById(R.id.fab);
                fab.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(t.getUrl()));
                        getContext().startActivity(browserIntent);
                    }
                });
                new Scrobbler(getContext()).execute(Stuff.TRACK_HERO, t.getUrl());

                hero.setTag(t.getUrl());
                if (imgUrl != null && !imgUrl.equals("")) {
                    setHero(t.getImageURL(ImageSize.MEDIUM)); //better set a blurred one
                }
            }
            return convertView;
        }

        void loadURL(int page){
            new Scrobbler(getContext()).execute(Stuff.GET_RECENTS, page+"");
        }

        void setHero (String imgUrl){
            if (imgUrl!= null && !imgUrl.equals(""))
                Picasso.with(getContext())
                        .load(imgUrl)
                        .fit()
                        .centerCrop()
                        .noFade()
                        .into(hero, new Callback() {
                            @Override
                            public void onSuccess() {
                                final CollapsingToolbarLayout ctl  = (CollapsingToolbarLayout) ((Activity)getContext()).findViewById(R.id.toolbar_layout);
                                final ListView list = (ListView) ((Activity)getContext()).findViewById(R.id.recents_list);
                                Bitmap b = ((BitmapDrawable)hero.getDrawable()).getBitmap();
                                Palette.generateAsync(b, new Palette.PaletteAsyncListener() {
                                    @Override
                                    public void onGenerated(Palette palette) {
                                        int c1 = palette.getDominantColor(getContext().getResources().getColor(R.color.colorPrimary));
                                        int c2 = palette.getDarkMutedColor(getContext().getResources().getColor(R.color.colorPrimaryDark));

                                        ctl.setContentScrimColor(c1);
                                        ctl.setStatusBarScrimColor(c2);

                                        list.setBackgroundColor(palette.getDarkMutedColor(getContext().getResources().getColor(android.R.color.background_dark)));
                                    }
                                });
                                /*
                                BlurTransform bt = new BlurTransform(getContext());
                                Bitmap b = ((BitmapDrawable)hero.getDrawable()).getBitmap();
                                b = bt.transform(b, list.getWidth(), list.getHeight());
                                BitmapDrawable bd = new BitmapDrawable(getContext().getResources(), b);
                                bd.setGravity(Gravity.CENTER | Gravity.CENTER_VERTICAL);
                                list.setBackground(bd);
                                */
                            }

                            @Override
                            public void onError() {

                            }
                        });
        }

        void populate(PaginatedResult<Track> res, int page){
            SwipeRefreshLayout refresh = (SwipeRefreshLayout)((Activity) getContext()).findViewById(R.id.swiperefresh);
            if (refresh != null)
                refresh.setRefreshing(false);
            if (page == 1)
                clear();
            for (Track t : res) {
                if (t != null) {
                    add(t);
                }
            }
            notifyDataSetChanged();
        }

        void markLoved(PaginatedResult<Track> res){
            ArrayList<Track> loved = new ArrayList<>(10);
            for (Track t : res) {
                if (t != null)
                    loved.add(t);
            }
            for (int i=0; i<loved.size(); i++) {
                for (int j=0; j < getCount(); j++)
                if (loved.get(i).getName().equals(getItem(j).getName()) &&
                        loved.get(i).getArtist().equals(getItem(j).getArtist())){
                    getItem(j).setLoved(true);
                }
            }
            notifyDataSetChanged();
        }

        private ImageButton.OnClickListener loveToggle = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ImageButton ib = (ImageButton)v;
                View parentRow = (View) v.getParent();
                ListView listView = (ListView) parentRow.getParent();
                final int pos = listView.getPositionForView(parentRow);


                if (v.getTag(R.id.recents_love) == FILLED){
                    new Scrobbler(getContext()).execute(Stuff.UNLOVE,
                            getItem(pos).getArtist(), getItem(pos).getName());
                    ib.setImageResource(R.drawable.ic_line_heart_disabled);
                    ib.setTag(R.id.recents_love, 0);
                } else {
                    new Scrobbler(getContext()).execute(Stuff.LOVE,
                            getItem(pos).getArtist(), getItem(pos).getName());
                    ib.setImageResource(R.drawable.ic_line_heart_enabled);
                    ib.setTag(R.id.recents_love, FILLED);
                }

            }
        };
    }