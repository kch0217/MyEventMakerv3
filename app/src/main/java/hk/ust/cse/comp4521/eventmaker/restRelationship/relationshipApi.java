//COMP4521  Kwok Chung Hin   20111831   chkwokad@ust.hk
//COMP4521  Kwok Tsz Ting 20119118  ttkwok@ust.hk
//COMP4521  Li Lok Him  20103470    lhliab@ust.hk
package hk.ust.cse.comp4521.eventmaker.restRelationship;

import java.util.ArrayList;

import hk.ust.cse.comp4521.eventmaker.Relationship.Relationship;
import hk.ust.cse.comp4521.eventmaker.Relationship.Relationship2;
import retrofit.Callback;
import retrofit.client.Response;
import retrofit.http.Body;
import retrofit.http.DELETE;
import retrofit.http.GET;
import retrofit.http.POST;
import retrofit.http.PUT;
import retrofit.http.Path;

/**
 * Created by User on 5/21/2015.
 */
public interface relationshipApi {
    //api for Retrofit
    @GET("/relationship")
    void getRelationships(Callback<ArrayList<Relationship>> callback);

    //
    @GET("/relationship/{id}")
    void getRelationship(@Path("id") String id, Callback<Relationship> callback);

    //
    @DELETE("/relationship/{id}")
    void deleteRelationship(@Path("id") String id, Callback<Response> callback);

    //
    @POST("/relationship")
    void addRelationship(@Body Relationship2 event, Callback<Response> callback);

    //
    @PUT("/relationship/{id}")
    void updateRelationship(@Body Relationship2 event, @Path("id") String id, Callback<Response> callback);

}
