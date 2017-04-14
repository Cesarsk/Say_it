package com.cesarsk.say_it.ui.fragments;


import android.app.Activity;
import android.app.ActivityOptions;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.Pair;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.cesarsk.say_it.ui.MainActivity;
import com.cesarsk.say_it.R;
import com.cesarsk.say_it.ui.PlayActivity;
import com.cesarsk.say_it.utility.SayItPair;
import com.cesarsk.say_it.utility.UtilitySharedPrefs;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import static android.speech.tts.TextToSpeech.QUEUE_FLUSH;

/**
 * A simple {@link Fragment} subclass.
 */
public class FavoritesFragment extends Fragment {

    ArrayList<Pair<String, String>> DeserializedFavs;
    RecyclerView recyclerView;
    Snackbar snackbar;

    public FavoritesFragment() {
        // Required empty public constructor
    }

    @Override
    public void onResume() {
        super.onResume();
        if(recyclerView != null){
            FavoritesAdapter adapter = (FavoritesAdapter) recyclerView.getAdapter();
            adapter.setFavorites(loadDeserializedFavs(getActivity()));
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        final View view = inflater.inflate(R.layout.fragment_favorites, container, false);

        DeserializedFavs = loadDeserializedFavs(getActivity());

        recyclerView = (RecyclerView) view.findViewById(R.id.favorites_list);
        recyclerView.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity());
        DefaultItemAnimator defaultItemAnimator = new DefaultItemAnimator();
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(getActivity(), linearLayoutManager.getOrientation());
        snackbar = Snackbar.make(view.findViewById(R.id.favorites_fragment_coordinator), "Removed Element from Favorites", (int) FavoritesAdapter.UNDO_TIMEOUT);


        final FavoritesAdapter adapter = new FavoritesAdapter(DeserializedFavs);
        recyclerView.setAdapter(adapter);

        ItemTouchHelper touchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {

            Drawable background;
            Drawable DeletedIcon;
            int DeletedIconMargin;
            boolean initiated;

            void init() {
                background = new ColorDrawable(ContextCompat.getColor(getActivity(), R.color.Red500));
                DeletedIcon = ContextCompat.getDrawable(getActivity(), R.drawable.ic_close_white_24dp);
                DeletedIconMargin = (int) getActivity().getResources().getDimension(R.dimen.deleted_icon_margin);
                initiated = true;
            }

            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                adapter.remove(viewHolder.getAdapterPosition());
                snackbar.show();
            }

            @Override
            public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {

                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);

                View itemView = viewHolder.itemView;

                // not sure why, but this method get's called for viewholder that are already swiped away
                if (viewHolder.getAdapterPosition() == -1) {
                    // not interested in those
                    return;
                }

                if (!initiated) {
                    init();
                }

                // draw red background
                background.setBounds(itemView.getRight() + (int) dX, itemView.getTop(), itemView.getRight(), itemView.getBottom());
                background.draw(c);

                int itemHeight = itemView.getBottom() - itemView.getTop();
                int intrinsicWidth = DeletedIcon.getIntrinsicWidth();
                int intrinsicHeight = DeletedIcon.getIntrinsicWidth();

                int xMarkLeft = itemView.getRight() - DeletedIconMargin - intrinsicWidth;
                int xMarkRight = itemView.getRight() - DeletedIconMargin;
                int xMarkTop = itemView.getTop() + (itemHeight - intrinsicHeight) / 2;
                int xMarkBottom = xMarkTop + intrinsicHeight;
                DeletedIcon.setBounds(xMarkLeft, xMarkTop, xMarkRight, xMarkBottom);

                DeletedIcon.draw(c);
            }
        });
        recyclerView.addItemDecoration(dividerItemDecoration);
        recyclerView.addItemDecoration(new RecyclerView.ItemDecoration() {

            // we want to cache this and not allocate anything repeatedly in the onDraw method
            Drawable background;
            boolean initiated;

            private void init() {
                background = new ColorDrawable(ContextCompat.getColor(getActivity(), R.color.Red500));
                initiated = true;
            }

            @Override
            public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {

                if (!initiated) {
                    init();
                }

                // only if animation is in progress
                if (parent.getItemAnimator().isRunning()) {

                    // some items might be animating down and some items might be animating up to close the gap left by the removed item
                    // this is not exclusive, both movement can be happening at the same time
                    // to reproduce this leave just enough items so the first one and the last one would be just a little off screen
                    // then remove one from the middle

                    // find first child with translationY > 0
                    // and last one with translationY < 0
                    // we're after a rect that is not covered in recycler-view views at this point in time
                    View lastViewComingDown = null;
                    View firstViewComingUp = null;

                    // this is fixed
                    int left = 0;
                    int right = parent.getWidth();

                    // this we need to find out
                    int top = 0;
                    int bottom = 0;

                    // find relevant translating views
                    int childCount = parent.getLayoutManager().getChildCount();
                    for (int i = 0; i < childCount; i++) {
                        View child = parent.getLayoutManager().getChildAt(i);
                        if (child.getTranslationY() < 0) {
                            // view is coming down
                            lastViewComingDown = child;
                        } else if (child.getTranslationY() > 0) {
                            // view is coming up
                            if (firstViewComingUp == null) {
                                firstViewComingUp = child;
                            }
                        }
                    }

                    if (lastViewComingDown != null && firstViewComingUp != null) {
                        // views are coming down AND going up to fill the void
                        top = lastViewComingDown.getBottom() + (int) lastViewComingDown.getTranslationY();
                        bottom = firstViewComingUp.getTop() + (int) firstViewComingUp.getTranslationY();
                    } else if (lastViewComingDown != null) {
                        // views are going down to fill the void
                        top = lastViewComingDown.getBottom() + (int) lastViewComingDown.getTranslationY();
                        bottom = lastViewComingDown.getBottom();
                    } else if (firstViewComingUp != null) {
                        // views are coming up to fill the void
                        top = firstViewComingUp.getTop();
                        bottom = firstViewComingUp.getTop() + (int) firstViewComingUp.getTranslationY();
                    }

                    background.setBounds(left, top, right, bottom);
                    background.draw(c);

                }
                super.onDraw(c, parent, state);
            }

        });
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setItemAnimator(defaultItemAnimator);
        touchHelper.attachToRecyclerView(recyclerView);

        return view;
    }

    public static ArrayList<Pair<String, String>> loadDeserializedFavs(Context context) {

        UtilitySharedPrefs.loadFavs(context);
        ArrayList<String> SerializedFavs = new ArrayList<>(MainActivity.FAVORITES);
        ArrayList<Pair<String, String>> DeserializedFavs = new ArrayList<>();
        Gson gson = new Gson();
        for (String element : SerializedFavs) {
            SayItPair pair = gson.fromJson(element, SayItPair.class);
            DeserializedFavs.add(pair);
        }

        Collections.sort(DeserializedFavs, new Comparator<Pair<String, String>>() {
            @Override
            public int compare(Pair<String, String> pair1, Pair<String, String> pair2) {
                return pair1.first.compareTo(pair2.first);
            }
        });

        return DeserializedFavs;

    }



    private class FavoritesAdapter extends RecyclerView.Adapter<FavoritesAdapter.ViewHolder> {

        public static final long UNDO_TIMEOUT = 3000; //Timeout prima che l'elemento venga cancellato definitivamente

        private ArrayList<Pair<String, String>> favorites;

        public Pair<String, String> getTemp_fav() {
            return temp_fav;
        }

        private Pair<String, String> temp_fav;

        public int getTemp_pos() {
            return temp_pos;
        }

        public void setTemp_pos(int temp_pos) {
            this.temp_pos = temp_pos;
        }

        private int temp_pos;

        public void setTemp_fav(Pair<String, String> temp_fav) {
            this.temp_fav = temp_fav;
        }

        /*private ArrayList<Pair<String, String>> pendingFavorites;
        private Handler handler = new Handler(); //Handler per gestire i Runnable per permettere l'UNDO con il Delay
        HashMap<Pair<String, String>, Runnable> pendingRunnables = new HashMap<>(); //HashMap che associa ad ogni elemento della lista un Runnable che aspetterà
        //3 secondi prima di cancellare l'elemento dalla lista.*/

        FavoritesAdapter(ArrayList<Pair<String, String>> favorites_list) {
            favorites = favorites_list;
            //pendingFavorites = new ArrayList<>();
        }

        class ViewHolder extends RecyclerView.ViewHolder {

            TextView wordTextView;
            TextView IPATextView;
            ImageButton QuickPlayBtn;
            ImageButton AddtoFavsBtn;

            ViewHolder(View itemView) {
                super(itemView);
                wordTextView = (TextView) itemView.findViewById(R.id.list_item_first_line);
                IPATextView = (TextView) itemView.findViewById(R.id.list_item_second_line);
                QuickPlayBtn = (ImageButton) itemView.findViewById(R.id.list_item_quickplay);
                AddtoFavsBtn = (ImageButton) itemView.findViewById(R.id.list_item_addToFavs);
            }
        }

        @Override
        public FavoritesAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

            LayoutInflater inflater = LayoutInflater.from(parent.getContext());

            View view = inflater.inflate(R.layout.list_item_generic, parent, false);

            return new ViewHolder(view);
        }


        @Override
        public void onBindViewHolder(final FavoritesAdapter.ViewHolder holder, int position) {

            final Pair<String, String> current_item = favorites.get(position);

                holder.QuickPlayBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //Cliccando su Play Button nella search result tab riproduce play.
                        if(MainActivity.DEFAULT_ACCENT.equals("0")) {
                            MainActivity.american_speaker_google.speak(holder.wordTextView.getText(), QUEUE_FLUSH, null, null);
                            
                        }
                        else if(MainActivity.DEFAULT_ACCENT.equals("1")) {
                            MainActivity.british_speaker_google.speak(holder.wordTextView.getText(),QUEUE_FLUSH,null,null);
                            
                        }
                    }
                });

            holder.wordTextView.setText(favorites.get(position).first);
            holder.IPATextView.setText(favorites.get(position).second);

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    final Intent play_activity_intent = new Intent(getActivity(), PlayActivity.class);
                    play_activity_intent.putExtra(PlayActivity.PLAY_WORD, holder.wordTextView.getText());
                    play_activity_intent.putExtra(PlayActivity.PLAY_IPA, holder.IPATextView.getText());
                    UtilitySharedPrefs.addHist(getActivity(), new SayItPair(holder.wordTextView.getText().toString(), holder.IPATextView.getText().toString()));
                    getActivity().startActivity(play_activity_intent, ActivityOptions.makeSceneTransitionAnimation((Activity) getActivity()).toBundle());
                }
            });

            final boolean favorite_flag = UtilitySharedPrefs.checkFavs(getActivity(), favorites.get(position).first);
            if (favorite_flag)
                holder.AddtoFavsBtn.setColorFilter(ContextCompat.getColor(getActivity(), R.color.RudolphsNose));

            holder.AddtoFavsBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!favorite_flag) {
                        add(holder);
                    }

                    if (favorite_flag) {
                        remove(holder.getAdapterPosition());
                    }
                }
            });

            snackbar.setAction("UNDO", new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    UtilitySharedPrefs.addFavs(getActivity(), temp_fav);
                    favorites = loadDeserializedFavs(getActivity());
                    notifyItemInserted(favorites.indexOf(temp_fav));
                }
            });
        }

        @Override
        public int getItemCount() {
            return favorites.size();
        }

        void remove(int pos) {

            temp_fav = favorites.get(pos);
            temp_pos = pos;

            UtilitySharedPrefs.removeFavs(getActivity(), favorites.get(pos));
            favorites = loadDeserializedFavs(getActivity());
            notifyItemRemoved(pos);
        }

        public void add(FavoritesAdapter.ViewHolder viewHolder) {
            UtilitySharedPrefs.addFavs(getActivity(), new Pair<>(viewHolder.wordTextView.getText().toString(), viewHolder.IPATextView.getText().toString()));
            Toast.makeText(getActivity(), "Added to Favorites", Toast.LENGTH_SHORT).show();
        }

        public void recover_temp_fav(){

        }

        public ArrayList<Pair<String, String>> getFavorites() {
            return favorites;
        }

        public void setFavorites(ArrayList<Pair<String, String>> favorites) {
            this.favorites = favorites;
            notifyDataSetChanged();
        }
    }
}