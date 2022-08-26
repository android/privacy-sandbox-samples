/*
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.adservices.samples.topics.sampleapp;

import android.adservices.topics.GetTopicsRequest;
import android.adservices.topics.GetTopicsResponse;
import android.adservices.topics.TopicsManager;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.OutcomeReceiver;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
@SuppressLint("NewApi")
/**
 * Android application activity for testing Topics API by sending a call to
 * the 'getTopics()' function onResume. When a result is received it will be displayed
 * on screen in a text box, as well as displaying a text box showing what the current
 * package name is for the application. This project can be build with 11 different
 * flavors, each of which will assign a different package name corresponding to a
 * different suite of possible Topics.
 */
public class MainActivity extends AppCompatActivity {

    //TextView to display results from getTopics call
    TextView results;
    //TextView to display current package name which influences returned topics
    TextView packageNameDisplay;

    //Button that launches settings UI
    private Button mSettingsAppButton;
    private static final String RB_SETTING_APP_INTENT = "android.adservices.ui.SETTINGS";

    //On app creation setup view as well as assign variables for TextViews to display results
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        results = (TextView) findViewById(R.id.textView);
        packageNameDisplay = (TextView) findViewById(R.id.textView2);
        mSettingsAppButton = (Button) findViewById(R.id.settings_app_launch_button);
        registerLaunchSettingsAppButton();
    }

    //On Application Resume, call getTopics code. This can be used to facilitate automating population of topics data
    @Override
    protected void onResume() {
        super.onResume();
        packageNameDisplay.setText(getBaseContext().getPackageName());
        TopicGetter();
    }

    //TopicGetter holds all of the setup and code for creating a TopicsManager and getTopics call
    public void TopicGetter() {
        Context mContext = getBaseContext();
        createTaxonomy();
        TopicsManager mTopicsManager = mContext.getSystemService(TopicsManager.class);
        Executor mExecutor = Executors.newCachedThreadPool();
        mTopicsManager.getTopics(GetTopicsRequest.create(),mExecutor,mCallback);
    }

    //onResult is called when getTopics successfully comes back with an answer
    OutcomeReceiver mCallback = new OutcomeReceiver<GetTopicsResponse, Exception>() {
        @Override
        public void onResult(@NonNull GetTopicsResponse result) {
            for (int i = 0; i < result.getTopics().size(); i++) {
                if(mTaxonomy.get((result.getTopics().get(i).getTopicId()))!=null)
                {
                    Log.i("Topic", mTaxonomy.get(result.getTopics().get(i).getTopicId()));
                    if (results.isEnabled()) {
                        //Receives the Topic ID and pulls the corresponding entry from the Topics Taxonomy
                        results.setText(mTaxonomy.get(result.getTopics().get(i).getTopicId()));
                    }
                }
                else
                {
                    Log.i("Topic", "Topic ID " + Integer.toString(result.getTopics().get(i).getTopicId()) + " was not found in Taxonomy");
                    results.setText("Returned with value but value not found in Taxonomy");
                }
            }
            if (result.getTopics().size() == 0) {
                Log.i("Topic", "Returned Empty");
                if (results.isEnabled()) {
                    results.setText("Returned Empty");
                }
            }
        }

        //onError should not be returned, even invalid topics callers should simply return empty
        @Override
        public void onError(@NonNull Exception error) {
            // Handle error
            Log.i("Topic", "Experienced an Error, and did not return successfully");
            if (results.isEnabled()) {
                results.setText("Returned An Error: " + error.getMessage());
            }
        }
    };

    //Does setup for button on screen that will launch settings UI to observe Topics info as an end user will
    private void registerLaunchSettingsAppButton() {
        mSettingsAppButton.setOnClickListener(
            new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Context context = getApplicationContext();
                    Intent activity2Intent = new Intent(RB_SETTING_APP_INTENT);
                    activity2Intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(activity2Intent);
                }
            });
    }

    public Map<Integer, String> mTaxonomy = new HashMap<Integer, String>();
    //Populating the hardcoded Taxonomy values so we can display topics instead of just IDs,
    void createTaxonomy() {
        mTaxonomy.put(10001, "	/Arts & Entertainment	");
        mTaxonomy.put(10002, "	/Arts & Entertainment/Acting & Theater	");
        mTaxonomy.put(10003, "	/Arts & Entertainment/Anime & Manga	");
        mTaxonomy.put(10004, "	/Arts & Entertainment/Cartoons	");
        mTaxonomy.put(10005, "	/Arts & Entertainment/Comics	");
        mTaxonomy.put(10006, "	/Arts & Entertainment/Concerts & Music Festivals	");
        mTaxonomy.put(10007, "	/Arts & Entertainment/Dance	");
        mTaxonomy.put(10008, "	/Arts & Entertainment/Entertainment Industry	");
        mTaxonomy.put(10009, "	/Arts & Entertainment/Fun & Trivia	");
        mTaxonomy.put(10010, "	/Arts & Entertainment/Fun & Trivia/Fun Tests & Silly Surveys	");
        mTaxonomy.put(10011, "	/Arts & Entertainment/Humor	");
        mTaxonomy.put(10012, "	/Arts & Entertainment/Humor/Funny Pictures & Videos	");
        mTaxonomy.put(10013, "	/Arts & Entertainment/Humor/Live Comedy	");
        mTaxonomy.put(10014, "	/Arts & Entertainment/Live Sporting Events	");
        mTaxonomy.put(10015, "	/Arts & Entertainment/Magic	");
        mTaxonomy.put(10016, "	/Arts & Entertainment/Movie Listings & Theater Showtimes	");
        mTaxonomy.put(10017, "	/Arts & Entertainment/Movies	");
        mTaxonomy.put(10018, "	/Arts & Entertainment/Movies/Action & Adventure Films	");
        mTaxonomy.put(10019, "	/Arts & Entertainment/Movies/Animated Films	");
        mTaxonomy.put(10020, "	/Arts & Entertainment/Movies/Comedy Films	");
        mTaxonomy.put(10021, "	/Arts & Entertainment/Movies/Cult & Indie Films	");
        mTaxonomy.put(10022, "	/Arts & Entertainment/Movies/Documentary Films	");
        mTaxonomy.put(10023, "	/Arts & Entertainment/Movies/Drama Films	");
        mTaxonomy.put(10024, "	/Arts & Entertainment/Movies/Family Films	");
        mTaxonomy.put(10025, "	/Arts & Entertainment/Movies/Horror Films	");
        mTaxonomy.put(10026, "	/Arts & Entertainment/Movies/Romance Films	");
        mTaxonomy.put(10027, "	/Arts & Entertainment/Movies/Thriller, Crime & Mystery Films	");
        mTaxonomy.put(10028, "	/Arts & Entertainment/Music & Audio	");
        mTaxonomy.put(10029, "	/Arts & Entertainment/Music & Audio/Blues	");
        mTaxonomy.put(10030, "	/Arts & Entertainment/Music & Audio/Classical Music	");
        mTaxonomy.put(10031, "	/Arts & Entertainment/Music & Audio/Country Music	");
        mTaxonomy.put(10032, "	/Arts & Entertainment/Music & Audio/Dance & Electronic Music	");
        mTaxonomy.put(10033, "	/Arts & Entertainment/Music & Audio/Folk & Traditional Music	");
        mTaxonomy.put(10034, "	/Arts & Entertainment/Music & Audio/Jazz	");
        mTaxonomy.put(10035, "	/Arts & Entertainment/Music & Audio/Music Streams & Downloads	");
        mTaxonomy.put(10036, "	/Arts & Entertainment/Music & Audio/Music Videos	");
        mTaxonomy.put(10037, "	/Arts & Entertainment/Music & Audio/Musical Instruments	");
        mTaxonomy.put(10038,
            "	/Arts & Entertainment/Music & Audio/Musical Instruments/Pianos & Keyboards	");
        mTaxonomy.put(10039, "	/Arts & Entertainment/Music & Audio/Pop Music	");
        mTaxonomy.put(10040, "	/Arts & Entertainment/Music & Audio/Radio	");
        mTaxonomy.put(10041, "	/Arts & Entertainment/Music & Audio/Radio/Talk Radio	");
        mTaxonomy.put(10042, "	/Arts & Entertainment/Music & Audio/Rap & Hip-Hop	");
        mTaxonomy.put(10043, "	/Arts & Entertainment/Music & Audio/Rock Music	");
        mTaxonomy.put(10044,
            "	/Arts & Entertainment/Music & Audio/Rock Music/Classic Rock & Oldies	");
        mTaxonomy.put(10045,
            "	/Arts & Entertainment/Music & Audio/Rock Music/Hard Rock & Progressive	");
        mTaxonomy.put(10046,
            "	/Arts & Entertainment/Music & Audio/Rock Music/Indie & Alternative Music	");
        mTaxonomy.put(10047, "	/Arts & Entertainment/Music & Audio/Samples & Sound Libraries	");
        mTaxonomy.put(10048, "	/Arts & Entertainment/Music & Audio/Soul & R&B	");
        mTaxonomy.put(10049, "	/Arts & Entertainment/Music & Audio/Soundtracks	");
        mTaxonomy.put(10050, "	/Arts & Entertainment/Music & Audio/World Music	");
        mTaxonomy.put(10051,
            "	/Arts & Entertainment/Music & Audio/World Music/Reggae & Caribbean Music	");
        mTaxonomy.put(10052, "	/Arts & Entertainment/Online Image Galleries	");
        mTaxonomy.put(10053, "	/Arts & Entertainment/Online Video	");
        mTaxonomy.put(10054, "	/Arts & Entertainment/Online Video/Live Video Streaming	");
        mTaxonomy.put(10055, "	/Arts & Entertainment/Online Video/Movie & TV Streaming	");
        mTaxonomy.put(10056, "	/Arts & Entertainment/Opera	");
        mTaxonomy.put(10057, "	/Arts & Entertainment/TV Guides & Reference	");
        mTaxonomy.put(10058, "	/Arts & Entertainment/TV Networks & Stations	");
        mTaxonomy.put(10059, "	/Arts & Entertainment/TV Shows & Programs	");
        mTaxonomy.put(10060, "	/Arts & Entertainment/TV Shows & Programs/TV Comedies	");
        mTaxonomy.put(10061,
            "	/Arts & Entertainment/TV Shows & Programs/TV Documentary & Nonfiction	");
        mTaxonomy.put(10062, "	/Arts & Entertainment/TV Shows & Programs/TV Dramas	");
        mTaxonomy.put(10063,
            "	/Arts & Entertainment/TV Shows & Programs/TV Dramas/TV Soap Operas	");
        mTaxonomy.put(10064,
            "	/Arts & Entertainment/TV Shows & Programs/TV Family-Oriented Shows	");
        mTaxonomy.put(10065, "	/Arts & Entertainment/TV Shows & Programs/TV Reality Shows	");
        mTaxonomy.put(10066,
            "	/Arts & Entertainment/TV Shows & Programs/TV Sci-Fi & Fantasy Shows	");
        mTaxonomy.put(10067, "	/Arts & Entertainment/Visual Art & Design	");
        mTaxonomy.put(10068, "	/Arts & Entertainment/Visual Art & Design/Design	");
        mTaxonomy.put(10069, "	/Arts & Entertainment/Visual Art & Design/Painting	");
        mTaxonomy.put(10070,
            "	/Arts & Entertainment/Visual Art & Design/Photographic & Digital Arts	");
        mTaxonomy.put(10071, "	/Autos & Vehicles	");
        mTaxonomy.put(10072, "	/Autos & Vehicles/Cargo Trucks & Trailers	");
        mTaxonomy.put(10073, "	/Autos & Vehicles/Classic Vehicles	");
        mTaxonomy.put(10074, "	/Autos & Vehicles/Custom & Performance Vehicles	");
        mTaxonomy.put(10075, "	/Autos & Vehicles/Gas Prices & Vehicle Fueling	");
        mTaxonomy.put(10076, "	/Autos & Vehicles/Motor Vehicles (By Type)	");
        mTaxonomy.put(10077, "	/Autos & Vehicles/Motor Vehicles (By Type)/Autonomous Vehicles	");
        mTaxonomy.put(10078, "	/Autos & Vehicles/Motor Vehicles (By Type)/Convertibles	");
        mTaxonomy.put(10079, "	/Autos & Vehicles/Motor Vehicles (By Type)/Coupes	");
        mTaxonomy.put(10080, "	/Autos & Vehicles/Motor Vehicles (By Type)/Hatchbacks	");
        mTaxonomy.put(10081,
            "	/Autos & Vehicles/Motor Vehicles (By Type)/Hybrid & Alternative Vehicles	");
        mTaxonomy.put(10082, "	/Autos & Vehicles/Motor Vehicles (By Type)/Luxury Vehicles	");
        mTaxonomy.put(10083,
            "	/Autos & Vehicles/Motor Vehicles (By Type)/Microcars & Subcompacts	");
        mTaxonomy.put(10084, "	/Autos & Vehicles/Motor Vehicles (By Type)/Motorcycles	");
        mTaxonomy.put(10085, "	/Autos & Vehicles/Motor Vehicles (By Type)/Off-Road Vehicles	");
        mTaxonomy.put(10086, "	/Autos & Vehicles/Motor Vehicles (By Type)/Pickup Trucks	");
        mTaxonomy.put(10087, "	/Autos & Vehicles/Motor Vehicles (By Type)/Scooters & Mopeds	");
        mTaxonomy.put(10088, "	/Autos & Vehicles/Motor Vehicles (By Type)/Sedans	");
        mTaxonomy.put(10089, "	/Autos & Vehicles/Motor Vehicles (By Type)/Station Wagons	");
        mTaxonomy.put(10090, "	/Autos & Vehicles/Motor Vehicles (By Type)/SUVs & Crossovers	");
        mTaxonomy.put(10091,
            "	/Autos & Vehicles/Motor Vehicles (By Type)/SUVs & Crossovers/Crossovers	");
        mTaxonomy.put(10092, "	/Autos & Vehicles/Motor Vehicles (By Type)/Vans & Minivans	");
        mTaxonomy.put(10093, "	/Autos & Vehicles/Towing & Roadside Assistance	");
        mTaxonomy.put(10094, "	/Autos & Vehicles/Vehicle & Traffic Safety	");
        mTaxonomy.put(10095, "	/Autos & Vehicles/Vehicle Parts & Accessories	");
        mTaxonomy.put(10096, "	/Autos & Vehicles/Vehicle Repair & Maintenance	");
        mTaxonomy.put(10097, "	/Autos & Vehicles/Vehicle Shopping	");
        mTaxonomy.put(10098, "	/Autos & Vehicles/Vehicle Shopping/Used Vehicles	");
        mTaxonomy.put(10099, "	/Autos & Vehicles/Vehicle Shows	");
        mTaxonomy.put(10100, "	/Beauty & Fitness	");
        mTaxonomy.put(10101, "	/Beauty & Fitness/Body Art	");
        mTaxonomy.put(10102, "	/Beauty & Fitness/Face & Body Care	");
        mTaxonomy.put(10103,
            "	/Beauty & Fitness/Face & Body Care/Antiperspirants, Deodorants & Body Sprays	");
        mTaxonomy.put(10104, "	/Beauty & Fitness/Face & Body Care/Bath & Body Products	");
        mTaxonomy.put(10105, "	/Beauty & Fitness/Face & Body Care/Clean Beauty	");
        mTaxonomy.put(10106, "	/Beauty & Fitness/Face & Body Care/Make-Up & Cosmetics	");
        mTaxonomy.put(10107, "	/Beauty & Fitness/Face & Body Care/Nail Care Products	");
        mTaxonomy.put(10108, "	/Beauty & Fitness/Face & Body Care/Perfumes & Fragrances	");
        mTaxonomy.put(10109, "	/Beauty & Fitness/Face & Body Care/Razors & Shavers	");
        mTaxonomy.put(10110, "	/Beauty & Fitness/Fashion & Style	");
        mTaxonomy.put(10111, "	/Beauty & Fitness/Fitness	");
        mTaxonomy.put(10112, "	/Beauty & Fitness/Fitness/Bodybuilding	");
        mTaxonomy.put(10113,
            "	/Beauty & Fitness/Fitness/Fitness Instruction & Personal Training	");
        mTaxonomy.put(10114, "	/Beauty & Fitness/Fitness/Fitness Technology Products	");
        mTaxonomy.put(10115, "	/Beauty & Fitness/Hair Care	");
        mTaxonomy.put(10116, "	/Books & Literature	");
        mTaxonomy.put(10117, "	/Books & Literature/Children's Literature	");
        mTaxonomy.put(10118, "	/Books & Literature/E-Books	");
        mTaxonomy.put(10119, "	/Books & Literature/Magazines	");
        mTaxonomy.put(10120, "	/Books & Literature/Poetry	");
        mTaxonomy.put(10121, "	/Business & Industrial	");
        mTaxonomy.put(10122, "	/Business & Industrial/Advertising & Marketing	");
        mTaxonomy.put(10123, "	/Business & Industrial/Advertising & Marketing/Sales	");
        mTaxonomy.put(10124, "	/Business & Industrial/Agriculture & Forestry	");
        mTaxonomy.put(10125, "	/Business & Industrial/Agriculture & Forestry/Food Production	");
        mTaxonomy.put(10126, "	/Business & Industrial/Automotive Industry	");
        mTaxonomy.put(10127, "	/Business & Industrial/Aviation Industry	");
        mTaxonomy.put(10128, "	/Business & Industrial/Business Operations	");
        mTaxonomy.put(10129,
            "	/Business & Industrial/Business Operations/Flexible Work Arrangements	");
        mTaxonomy.put(10130, "	/Business & Industrial/Business Operations/Human Resources	");
        mTaxonomy.put(10131, "	/Business & Industrial/Commercial Lending	");
        mTaxonomy.put(10132, "	/Business & Industrial/Construction & Maintenance	");
        mTaxonomy.put(10133,
            "	/Business & Industrial/Construction & Maintenance/Civil Engineering	");
        mTaxonomy.put(10134, "	/Business & Industrial/Defense Industry	");
        mTaxonomy.put(10135, "	/Business & Industrial/Energy & Utilities	");
        mTaxonomy.put(10136,
            "	/Business & Industrial/Energy & Utilities/Water Supply & Treatment	");
        mTaxonomy.put(10137, "	/Business & Industrial/Hospitality Industry	");
        mTaxonomy.put(10138, "	/Business & Industrial/Manufacturing	");
        mTaxonomy.put(10139, "	/Business & Industrial/Metals & Mining	");
        mTaxonomy.put(10140, "	/Business & Industrial/MLM & Business Opportunities	");
        mTaxonomy.put(10141, "	/Business & Industrial/Pharmaceuticals & Biotech	");
        mTaxonomy.put(10142, "	/Business & Industrial/Printing & Publishing	");
        mTaxonomy.put(10143, "	/Business & Industrial/Retail Trade	");
        mTaxonomy.put(10144, "	/Business & Industrial/Venture Capital	");
        mTaxonomy.put(10145, "	/Computers & Electronics	");
        mTaxonomy.put(10146, "	/Computers & Electronics/Computer Peripherals	");
        mTaxonomy.put(10147, "	/Computers & Electronics/Computer Peripherals/Printers	");
        mTaxonomy.put(10148, "	/Computers & Electronics/Computer Security	");
        mTaxonomy.put(10149, "	/Computers & Electronics/Computer Security/Antivirus & Malware	");
        mTaxonomy.put(10150, "	/Computers & Electronics/Computer Security/Network Security	");
        mTaxonomy.put(10151, "	/Computers & Electronics/Consumer Electronics	");
        mTaxonomy.put(10152,
            "	/Computers & Electronics/Consumer Electronics/Cameras & Camcorders	");
        mTaxonomy.put(10153, "	/Computers & Electronics/Consumer Electronics/GPS & Navigation	");
        mTaxonomy.put(10154, "	/Computers & Electronics/Consumer Electronics/Home Automation	");
        mTaxonomy.put(10155,
            "	/Computers & Electronics/Consumer Electronics/Home Theater Systems	");
        mTaxonomy.put(10156,
            "	/Computers & Electronics/Consumer Electronics/MP3 & Portable Media Players	");
        mTaxonomy.put(10157,
            "	/Computers & Electronics/Consumer Electronics/Wearable Technology	");
        mTaxonomy.put(10158, "	/Computers & Electronics/Data Backup & Recovery	");
        mTaxonomy.put(10159, "	/Computers & Electronics/Desktop Computers	");
        mTaxonomy.put(10160, "	/Computers & Electronics/Laptops & Notebooks	");
        mTaxonomy.put(10161, "	/Computers & Electronics/Networking	");
        mTaxonomy.put(10162,
            "	/Computers & Electronics/Networking/Distributed & Cloud Computing	");
        mTaxonomy.put(10163, "	/Computers & Electronics/Programming	");
        mTaxonomy.put(10164, "	/Computers & Electronics/Software	");
        mTaxonomy.put(10165, "	/Computers & Electronics/Software/Audio & Music Software	");
        mTaxonomy.put(10166,
            "	/Computers & Electronics/Software/Business & Productivity Software	");
        mTaxonomy.put(10167,
            "	/Computers & Electronics/Software/Business & Productivity Software/Calendar & Scheduling Software	");
        mTaxonomy.put(10168,
            "	/Computers & Electronics/Software/Business & Productivity Software/Collaboration & Conferencing Software	");
        mTaxonomy.put(10169,
            "	/Computers & Electronics/Software/Business & Productivity Software/Presentation Software	");
        mTaxonomy.put(10170,
            "	/Computers & Electronics/Software/Business & Productivity Software/Spreadsheet Software	");
        mTaxonomy.put(10171,
            "	/Computers & Electronics/Software/Business & Productivity Software/Word Processing Software	");
        mTaxonomy.put(10172, "	/Computers & Electronics/Software/Desktop Publishing	");
        mTaxonomy.put(10173, "	/Computers & Electronics/Software/Desktop Publishing/Fonts	");
        mTaxonomy.put(10174, "	/Computers & Electronics/Software/Download Managers	");
        mTaxonomy.put(10175, "	/Computers & Electronics/Software/Freeware & Shareware	");
        mTaxonomy.put(10176, "	/Computers & Electronics/Software/Graphics & Animation Software	");
        mTaxonomy.put(10177,
            "	/Computers & Electronics/Software/Intelligent Personal Assistants	");
        mTaxonomy.put(10178, "	/Computers & Electronics/Software/Media Players	");
        mTaxonomy.put(10179, "	/Computers & Electronics/Software/Monitoring Software	");
        mTaxonomy.put(10180, "	/Computers & Electronics/Software/Operating Systems	");
        mTaxonomy.put(10181, "	/Computers & Electronics/Software/Photo & Video Software	");
        mTaxonomy.put(10182,
            "	/Computers & Electronics/Software/Photo & Video Software/Photo Software	");
        mTaxonomy.put(10183,
            "	/Computers & Electronics/Software/Photo & Video Software/Video Software	");
        mTaxonomy.put(10184, "	/Computers & Electronics/Software/Software Utilities	");
        mTaxonomy.put(10185, "	/Computers & Electronics/Software/Web Browsers	");
        mTaxonomy.put(10186, "	/Finance	");
        mTaxonomy.put(10187, "	/Finance/Accounting & Auditing	");
        mTaxonomy.put(10188, "	/Finance/Accounting & Auditing/Tax Preparation & Planning	");
        mTaxonomy.put(10189, "	/Finance/Banking	");
        mTaxonomy.put(10190, "	/Finance/Banking/Money Transfer & Wire Services	");
        mTaxonomy.put(10191, "	/Finance/Credit & Lending	");
        mTaxonomy.put(10192, "	/Finance/Credit & Lending/Credit Cards	");
        mTaxonomy.put(10193, "	/Finance/Credit & Lending/Home Financing	");
        mTaxonomy.put(10194, "	/Finance/Credit & Lending/Personal Loans	");
        mTaxonomy.put(10195, "	/Finance/Credit & Lending/Student Loans & College Financing	");
        mTaxonomy.put(10196, "	/Finance/Financial Planning & Management	");
        mTaxonomy.put(10197, "	/Finance/Financial Planning & Management/Retirement & Pension	");
        mTaxonomy.put(10198, "	/Finance/Grants, Scholarships & Financial Aid	");
        mTaxonomy.put(10199,
            "	/Finance/Grants, Scholarships & Financial Aid/Study Grants & Scholarships	");
        mTaxonomy.put(10200, "	/Finance/Insurance	");
        mTaxonomy.put(10201, "	/Finance/Insurance/Auto Insurance	");
        mTaxonomy.put(10202, "	/Finance/Insurance/Health Insurance	");
        mTaxonomy.put(10203, "	/Finance/Insurance/Home Insurance	");
        mTaxonomy.put(10204, "	/Finance/Insurance/Life Insurance	");
        mTaxonomy.put(10205, "	/Finance/Insurance/Travel Insurance	");
        mTaxonomy.put(10206, "	/Finance/Investing	");
        mTaxonomy.put(10207, "	/Finance/Investing/Commodities & Futures Trading	");
        mTaxonomy.put(10208, "	/Finance/Investing/Currencies & Foreign Exchange	");
        mTaxonomy.put(10209, "	/Finance/Investing/Hedge Funds	");
        mTaxonomy.put(10210, "	/Finance/Investing/Mutual Funds	");
        mTaxonomy.put(10211, "	/Finance/Investing/Stocks & Bonds	");
        mTaxonomy.put(10212, "	/Food & Drink	");
        mTaxonomy.put(10213, "	/Food & Drink/Cooking & Recipes	");
        mTaxonomy.put(10214, "	/Food & Drink/Cooking & Recipes/BBQ & Grilling	");
        mTaxonomy.put(10215, "	/Food & Drink/Cooking & Recipes/Cuisines	");
        mTaxonomy.put(10216, "	/Food & Drink/Cooking & Recipes/Cuisines/Vegetarian Cuisine	");
        mTaxonomy.put(10217,
            "	/Food & Drink/Cooking & Recipes/Cuisines/Vegetarian Cuisine/Vegan Cuisine	");
        mTaxonomy.put(10218, "	/Food & Drink/Cooking & Recipes/Healthy Eating	");
        mTaxonomy.put(10219, "	/Food & Drink/Food & Grocery Retailers	");
        mTaxonomy.put(10220, "	/Games	");
        mTaxonomy.put(10221, "	/Games/Arcade & Coin-Op Games	");
        mTaxonomy.put(10222, "	/Games/Billiards	");
        mTaxonomy.put(10223, "	/Games/Board Games	");
        mTaxonomy.put(10224, "	/Games/Board Games/Chess & Abstract Strategy Games	");
        mTaxonomy.put(10225, "	/Games/Card Games	");
        mTaxonomy.put(10226, "	/Games/Card Games/Collectible Card Games	");
        mTaxonomy.put(10227, "	/Games/Computer & Video Games	");
        mTaxonomy.put(10228, "	/Games/Computer & Video Games/Action & Platform Games	");
        mTaxonomy.put(10229, "	/Games/Computer & Video Games/Adventure Games	");
        mTaxonomy.put(10230, "	/Games/Computer & Video Games/Casual Games	");
        mTaxonomy.put(10231, "	/Games/Computer & Video Games/Competitive Video Gaming	");
        mTaxonomy.put(10232, "	/Games/Computer & Video Games/Driving & Racing Games	");
        mTaxonomy.put(10233, "	/Games/Computer & Video Games/Fighting Games	");
        mTaxonomy.put(10234, "	/Games/Computer & Video Games/Gaming Reference & Reviews	");
        mTaxonomy.put(10235,
            "	/Games/Computer & Video Games/Gaming Reference & Reviews/Video Game Cheats & Hints	");
        mTaxonomy.put(10236, "	/Games/Computer & Video Games/Massively Multiplayer Games	");
        mTaxonomy.put(10237, "	/Games/Computer & Video Games/Music & Dance Games	");
        mTaxonomy.put(10238, "	/Games/Computer & Video Games/Sandbox Games	");
        mTaxonomy.put(10239, "	/Games/Computer & Video Games/Shooter Games	");
        mTaxonomy.put(10240, "	/Games/Computer & Video Games/Simulation Games	");
        mTaxonomy.put(10241,
            "	/Games/Computer & Video Games/Simulation Games/Business & Tycoon Games	");
        mTaxonomy.put(10242,
            "	/Games/Computer & Video Games/Simulation Games/City Building Games	");
        mTaxonomy.put(10243,
            "	/Games/Computer & Video Games/Simulation Games/Life Simulation Games	");
        mTaxonomy.put(10244,
            "	/Games/Computer & Video Games/Simulation Games/Vehicle Simulators	");
        mTaxonomy.put(10245, "	/Games/Computer & Video Games/Sports Games	");
        mTaxonomy.put(10246,
            "	/Games/Computer & Video Games/Sports Games/Sports Management Games	");
        mTaxonomy.put(10247, "	/Games/Computer & Video Games/Strategy Games	");
        mTaxonomy.put(10248, "	/Games/Computer & Video Games/Video Game Mods & Add-Ons	");
        mTaxonomy.put(10249, "	/Games/Educational Games	");
        mTaxonomy.put(10250, "	/Games/Family-Oriented Games & Activities	");
        mTaxonomy.put(10251, "	/Games/Family-Oriented Games & Activities/Drawing & Coloring	");
        mTaxonomy.put(10252,
            "	/Games/Family-Oriented Games & Activities/Dress-Up & Fashion Games	");
        mTaxonomy.put(10253, "	/Games/Puzzles & Brainteasers	");
        mTaxonomy.put(10254, "	/Games/Roleplaying Games	");
        mTaxonomy.put(10255, "	/Games/Table Tennis	");
        mTaxonomy.put(10256, "	/Games/Tile Games	");
        mTaxonomy.put(10257, "	/Games/Word Games	");
        mTaxonomy.put(10258, "	/Hobbies & Leisure	");
        mTaxonomy.put(10259, "	/Hobbies & Leisure/Anniversaries	");
        mTaxonomy.put(10260, "	/Hobbies & Leisure/Birthdays & Name Days	");
        mTaxonomy.put(10261, "	/Hobbies & Leisure/Diving & Underwater Activities	");
        mTaxonomy.put(10262, "	/Hobbies & Leisure/Fiber & Textile Arts	");
        mTaxonomy.put(10263, "	/Hobbies & Leisure/Outdoors	");
        mTaxonomy.put(10264, "	/Hobbies & Leisure/Outdoors/Fishing	");
        mTaxonomy.put(10265, "	/Hobbies & Leisure/Outdoors/Hunting & Shooting	");
        mTaxonomy.put(10266, "	/Hobbies & Leisure/Paintball	");
        mTaxonomy.put(10267, "	/Hobbies & Leisure/Radio Control & Modeling	");
        mTaxonomy.put(10268, "	/Hobbies & Leisure/Weddings	");
        mTaxonomy.put(10269, "	/Home & Garden	");
        mTaxonomy.put(10270, "	/Home & Garden/Gardening	");
        mTaxonomy.put(10271, "	/Home & Garden/Home & Interior Decor	");
        mTaxonomy.put(10272, "	/Home & Garden/Home Appliances	");
        mTaxonomy.put(10273, "	/Home & Garden/Home Improvement	");
        mTaxonomy.put(10274, "	/Home & Garden/Home Safety & Security	");
        mTaxonomy.put(10275, "	/Home & Garden/Household Supplies	");
        mTaxonomy.put(10276, "	/Home & Garden/Landscape Design	");
        mTaxonomy.put(10277, "	/Internet & Telecom	");
        mTaxonomy.put(10278, "	/Internet & Telecom/Email & Messaging	");
        mTaxonomy.put(10279, "	/Internet & Telecom/Email & Messaging/Email	");
        mTaxonomy.put(10280, "	/Internet & Telecom/Email & Messaging/Text & Instant Messaging	");
        mTaxonomy.put(10281, "	/Internet & Telecom/Email & Messaging/Voice & Video Chat	");
        mTaxonomy.put(10282, "	/Internet & Telecom/ISPs	");
        mTaxonomy.put(10283, "	/Internet & Telecom/Phone Service Providers	");
        mTaxonomy.put(10284, "	/Internet & Telecom/Ringtones & Mobile Themes	");
        mTaxonomy.put(10285, "	/Internet & Telecom/Search Engines	");
        mTaxonomy.put(10286, "	/Internet & Telecom/Smart Phones	");
        mTaxonomy.put(10287, "	/Internet & Telecom/Teleconferencing	");
        mTaxonomy.put(10288, "	/Internet & Telecom/Web Apps & Online Tools	");
        mTaxonomy.put(10289, "	/Internet & Telecom/Web Services	");
        mTaxonomy.put(10290, "	/Internet & Telecom/Web Services/Cloud Storage	");
        mTaxonomy.put(10291, "	/Internet & Telecom/Web Services/Web Design & Development	");
        mTaxonomy.put(10292, "	/Internet & Telecom/Web Services/Web Hosting	");
        mTaxonomy.put(10293, "	/Jobs & Education	");
        mTaxonomy.put(10294, "	/Jobs & Education/Education	");
        mTaxonomy.put(10295, "	/Jobs & Education/Education/Academic Conferences & Publications	");
        mTaxonomy.put(10296, "	/Jobs & Education/Education/Colleges & Universities	");
        mTaxonomy.put(10297, "	/Jobs & Education/Education/Distance Learning	");
        mTaxonomy.put(10298, "	/Jobs & Education/Education/Early Childhood Education	");
        mTaxonomy.put(10299, "	/Jobs & Education/Education/Early Childhood Education/Preschool	");
        mTaxonomy.put(10300, "	/Jobs & Education/Education/Homeschooling	");
        mTaxonomy.put(10301, "	/Jobs & Education/Education/Standardized & Admissions Tests	");
        mTaxonomy.put(10302, "	/Jobs & Education/Education/Teaching & Classroom Resources	");
        mTaxonomy.put(10303, "	/Jobs & Education/Education/Vocational & Continuing Education	");
        mTaxonomy.put(10304, "	/Jobs & Education/Jobs	");
        mTaxonomy.put(10305, "	/Jobs & Education/Jobs/Career Resources & Planning	");
        mTaxonomy.put(10306, "	/Jobs & Education/Jobs/Job Listings	");
        mTaxonomy.put(10307, "	/Law & Government	");
        mTaxonomy.put(10308, "	/Law & Government/Crime & Justice	");
        mTaxonomy.put(10309, "	/Law & Government/Legal	");
        mTaxonomy.put(10310, "	/Law & Government/Legal/Legal Services	");
        mTaxonomy.put(10311, "	/News	");
        mTaxonomy.put(10312, "	/News/Economy News	");
        mTaxonomy.put(10313, "	/News/Local News	");
        mTaxonomy.put(10314, "	/News/Mergers & Acquisitions	");
        mTaxonomy.put(10315, "	/News/Newspapers	");
        mTaxonomy.put(10316, "	/News/Politics	");
        mTaxonomy.put(10317, "	/News/Sports News	");
        mTaxonomy.put(10318, "	/News/Weather	");
        mTaxonomy.put(10319, "	/News/World News	");
        mTaxonomy.put(10320, "	/Online Communities	");
        mTaxonomy.put(10321, "	/Online Communities/Clip Art & Animated GIFs	");
        mTaxonomy.put(10322, "	/Online Communities/Dating & Personals	");
        mTaxonomy.put(10323, "	/Online Communities/Feed Aggregation & Social Bookmarking	");
        mTaxonomy.put(10324, "	/Online Communities/File Sharing & Hosting	");
        mTaxonomy.put(10325, "	/Online Communities/Forum & Chat Providers	");
        mTaxonomy.put(10326, "	/Online Communities/Microblogging	");
        mTaxonomy.put(10327, "	/Online Communities/Photo & Video Sharing	");
        mTaxonomy.put(10328, "	/Online Communities/Photo & Video Sharing/Photo & Image Sharing	");
        mTaxonomy.put(10329, "	/Online Communities/Photo & Video Sharing/Video Sharing	");
        mTaxonomy.put(10330, "	/Online Communities/Skins, Themes & Wallpapers	");
        mTaxonomy.put(10331, "	/Online Communities/Social Network Apps & Add-Ons	");
        mTaxonomy.put(10332, "	/Online Communities/Social Networks	");
        mTaxonomy.put(10333, "	/People & Society	");
        mTaxonomy.put(10334, "	/People & Society/Family & Relationships	");
        mTaxonomy.put(10335, "	/People & Society/Family & Relationships/Ancestry & Genealogy	");
        mTaxonomy.put(10336, "	/People & Society/Family & Relationships/Marriage	");
        mTaxonomy.put(10337, "	/People & Society/Family & Relationships/Parenting	");
        mTaxonomy.put(10338, "	/People & Society/Family & Relationships/Parenting/Adoption	");
        mTaxonomy.put(10339,
            "	/People & Society/Family & Relationships/Parenting/Babies & Toddlers	");
        mTaxonomy.put(10340,
            "	/People & Society/Family & Relationships/Parenting/Child Internet Safety	");
        mTaxonomy.put(10341, "	/People & Society/Family & Relationships/Romance	");
        mTaxonomy.put(10342, "	/People & Society/Science Fiction & Fantasy	");
        mTaxonomy.put(10343, "	/Pets & Animals	");
        mTaxonomy.put(10344, "	/Pets & Animals/Pet Food & Pet Care Supplies	");
        mTaxonomy.put(10345, "	/Pets & Animals/Pets	");
        mTaxonomy.put(10346, "	/Pets & Animals/Pets/Birds	");
        mTaxonomy.put(10347, "	/Pets & Animals/Pets/Cats	");
        mTaxonomy.put(10348, "	/Pets & Animals/Pets/Dogs	");
        mTaxonomy.put(10349, "	/Pets & Animals/Pets/Fish & Aquaria	");
        mTaxonomy.put(10350, "	/Pets & Animals/Pets/Reptiles & Amphibians	");
        mTaxonomy.put(10351, "	/Pets & Animals/Veterinarians	");
        mTaxonomy.put(10352, "	/Real Estate	");
        mTaxonomy.put(10353, "	/Real Estate/Lots & Land	");
        mTaxonomy.put(10354, "	/Real Estate/Timeshares & Vacation Properties	");
        mTaxonomy.put(10355, "	/Reference	");
        mTaxonomy.put(10356, "	/Reference/Business & Personal Listings	");
        mTaxonomy.put(10357, "	/Reference/General Reference	");
        mTaxonomy.put(10358, "	/Reference/General Reference/Calculators & Reference Tools	");
        mTaxonomy.put(10359, "	/Reference/General Reference/Dictionaries & Encyclopedias	");
        mTaxonomy.put(10360, "	/Reference/General Reference/Educational Resources	");
        mTaxonomy.put(10361, "	/Reference/General Reference/How-To, DIY & Expert Content	");
        mTaxonomy.put(10362, "	/Reference/General Reference/Time & Calendars	");
        mTaxonomy.put(10363, "	/Reference/Language Resources	");
        mTaxonomy.put(10364, "	/Reference/Language Resources/Foreign Language Study	");
        mTaxonomy.put(10365, "	/Reference/Language Resources/Translation Tools & Resources	");
        mTaxonomy.put(10366, "	/Reference/Maps	");
        mTaxonomy.put(10367, "	/Science	");
        mTaxonomy.put(10368, "	/Science/Augmented & Virtual Reality	");
        mTaxonomy.put(10369, "	/Science/Biological Sciences	");
        mTaxonomy.put(10370, "	/Science/Biological Sciences/Genetics	");
        mTaxonomy.put(10371, "	/Science/Chemistry	");
        mTaxonomy.put(10372, "	/Science/Ecology & Environment	");
        mTaxonomy.put(10373, "	/Science/Geology	");
        mTaxonomy.put(10374, "	/Science/Machine Learning & Artificial Intelligence	");
        mTaxonomy.put(10375, "	/Science/Mathematics	");
        mTaxonomy.put(10376, "	/Science/Physics	");
        mTaxonomy.put(10377, "	/Science/Robotics	");
        mTaxonomy.put(10378, "	/Shopping	");
        mTaxonomy.put(10379, "	/Shopping/Antiques & Collectibles	");
        mTaxonomy.put(10380, "	/Shopping/Apparel	");
        mTaxonomy.put(10381, "	/Shopping/Apparel/Children's Clothing	");
        mTaxonomy.put(10382, "	/Shopping/Apparel/Costumes	");
        mTaxonomy.put(10383, "	/Shopping/Apparel/Men's Clothing	");
        mTaxonomy.put(10384, "	/Shopping/Apparel/Women's Clothing	");
        mTaxonomy.put(10385, "	/Shopping/Classifieds	");
        mTaxonomy.put(10386, "	/Shopping/Consumer Resources	");
        mTaxonomy.put(10387, "	/Shopping/Consumer Resources/Coupons & Discount Offers	");
        mTaxonomy.put(10388, "	/Shopping/Consumer Resources/Loyalty Cards & Programs	");
        mTaxonomy.put(10389, "	/Shopping/Consumer Resources/Technical Support & Repair	");
        mTaxonomy.put(10390, "	/Shopping/Flowers	");
        mTaxonomy.put(10391, "	/Shopping/Greeting Cards	");
        mTaxonomy.put(10392, "	/Shopping/Party & Holiday Supplies	");
        mTaxonomy.put(10393, "	/Shopping/Shopping Portals	");
        mTaxonomy.put(10394, "	/Sports	");
        mTaxonomy.put(10395, "	/Sports/American Football	");
        mTaxonomy.put(10396, "	/Sports/Australian Football	");
        mTaxonomy.put(10397, "	/Sports/Auto Racing	");
        mTaxonomy.put(10398, "	/Sports/Baseball	");
        mTaxonomy.put(10399, "	/Sports/Basketball	");
        mTaxonomy.put(10400, "	/Sports/Bowling	");
        mTaxonomy.put(10401, "	/Sports/Boxing	");
        mTaxonomy.put(10402, "	/Sports/Cheerleading	");
        mTaxonomy.put(10403, "	/Sports/College Sports	");
        mTaxonomy.put(10404, "	/Sports/Cricket	");
        mTaxonomy.put(10405, "	/Sports/Cycling	");
        mTaxonomy.put(10406, "	/Sports/Equestrian	");
        mTaxonomy.put(10407, "	/Sports/Extreme Sports	");
        mTaxonomy.put(10408, "	/Sports/Extreme Sports/Climbing & Mountaineering	");
        mTaxonomy.put(10409, "	/Sports/Fantasy Sports	");
        mTaxonomy.put(10410, "	/Sports/Golf	");
        mTaxonomy.put(10411, "	/Sports/Gymnastics	");
        mTaxonomy.put(10412, "	/Sports/Hockey	");
        mTaxonomy.put(10413, "	/Sports/Ice Skating	");
        mTaxonomy.put(10414, "	/Sports/Martial Arts	");
        mTaxonomy.put(10415, "	/Sports/Motorcycle Racing	");
        mTaxonomy.put(10416, "	/Sports/Olympics	");
        mTaxonomy.put(10417, "	/Sports/Rugby	");
        mTaxonomy.put(10418, "	/Sports/Running & Walking	");
        mTaxonomy.put(10419, "	/Sports/Skiing & Snowboarding	");
        mTaxonomy.put(10420, "	/Sports/Soccer	");
        mTaxonomy.put(10421, "	/Sports/Surfing	");
        mTaxonomy.put(10422, "	/Sports/Swimming	");
        mTaxonomy.put(10423, "	/Sports/Tennis	");
        mTaxonomy.put(10424, "	/Sports/Track & Field	");
        mTaxonomy.put(10425, "	/Sports/Volleyball	");
        mTaxonomy.put(10426, "	/Sports/Wrestling	");
        mTaxonomy.put(10427, "	/Travel & Transportation	");
        mTaxonomy.put(10428, "	/Travel & Transportation/Adventure Travel	");
        mTaxonomy.put(10429, "	/Travel & Transportation/Air Travel	");
        mTaxonomy.put(10430, "	/Travel & Transportation/Business Travel	");
        mTaxonomy.put(10431, "	/Travel & Transportation/Car Rentals	");
        mTaxonomy.put(10432, "	/Travel & Transportation/Cruises & Charters	");
        mTaxonomy.put(10433, "	/Travel & Transportation/Family Travel	");
        mTaxonomy.put(10434, "	/Travel & Transportation/Honeymoons & Romantic Getaways	");
        mTaxonomy.put(10435, "	/Travel & Transportation/Hotels & Accommodations	");
        mTaxonomy.put(10436, "	/Travel & Transportation/Long Distance Bus & Rail	");
        mTaxonomy.put(10437, "	/Travel & Transportation/Low Cost & Last Minute Travel	");
        mTaxonomy.put(10438, "	/Travel & Transportation/Luggage & Travel Accessories	");
        mTaxonomy.put(10439, "	/Travel & Transportation/Tourist Destinations	");
        mTaxonomy.put(10440, "	/Travel & Transportation/Tourist Destinations/Beaches & Islands	");
        mTaxonomy.put(10441,
            "	/Travel & Transportation/Tourist Destinations/Regional Parks & Gardens	");
        mTaxonomy.put(10442, "	/Travel & Transportation/Tourist Destinations/Theme Parks	");
        mTaxonomy.put(10443,
            "	/Travel & Transportation/Tourist Destinations/Zoos, Aquariums & Preserves	");
        mTaxonomy.put(10444, "	/Travel & Transportation/Traffic & Route Planners	");
        mTaxonomy.put(10445, "	/Travel & Transportation/Travel Agencies & Services	");
        mTaxonomy.put(10446, "	/Travel & Transportation/Travel Guides & Travelogues	");
    }
}