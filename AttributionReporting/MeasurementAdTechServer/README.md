# Measurement Demo Server

This is a demo server which works in-tandem with the provided sample app to
showcase the measurement workflow from AdTech perspective.

## Supported Features

- Source Registration
   - POST: `/source?ad_id=<ad_id>`
- Trigger Registration
   - POST: `/trigger?ad_id=<ad_id>`
- Event Report Receipt/View
   - POST: `/.well-known/attribution-reporting/report-attribution`
   - GET: `/event-reports`
- Aggregate Report Receipt/View
   - POST: `/.well-known/attribution-reporting/report-aggregate-attribution`
   - GET: `/aggregate-reports`

## Response Configuration

This server loads the json files located at `src/main/resources/data` for generating responses.

- `sources.json`: Responses for source registration requests
- `triggers.json`: Responses for trigger registration requests


## How to run

### Local Run

`./gradlew bootRun`

### Deploy to Google Cloud - App Engine

#### Set the Google Cloud project id

In `build.gradle.kts` :
- Set `appengine.deploy.projectId = 'project-id'`

   OR

- Set `appengine.deploy.projectId = "GCLOUD_CONFIG"` to use the project from gcloud config

#### Deploy

`./gradlew appengineDeploy`

### Update Response Configuration

Update the base URLs for `Attribution-Reporting-Redirect` in `sources.json` and `triggers.json`. i.e., Replace the default `https://127.0.0.1:8080` value with the URL of the deployed server.
