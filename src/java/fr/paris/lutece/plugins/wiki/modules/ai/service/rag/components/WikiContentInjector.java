/*
 * Copyright (c) 2002-2026, City of Paris
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  1. Redistributions of source code must retain the above copyright notice
 *     and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright notice
 *     and the following disclaimer in the documentation and/or other materials
 *     provided with the distribution.
 *
 *  3. Neither the name of 'Mairie de Paris' nor 'Lutece' nor the names of its
 *     contributors may be used to endorse or promote products derived from
 *     this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * License 1.0
 */
package fr.paris.lutece.plugins.wiki.modules.ai.service.rag.components;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.injector.ContentInjector;

public class WikiContentInjector implements ContentInjector
{
    private static final String PROMPT_WITH_CONTEXT = "{{userMessage}}\n\n" + "<rag_context>\n" + "You have access to the following sources to answer:\n\n"
            + "{{contents}}\n\n" + "MANDATORY RULES:\n" + "1. AMBIGUITY DETECTION:\n"
            + " - If sources refer to MULTIPLE DIFFERENT subjects/contexts with the same term, you MUST ask the user to clarify BEFORE answering\n"
            + " - Clarification example: \"I found information about X different topics regarding '[term]'. Are you referring to:\n"
            + " 1) [First context briefly described]\n" + " 2) [Second context briefly described]\n" + " Could you please clarify your question?\"\n"
            + " - Only answer both contexts if the question is clearly general (e.g., \"explain all types of...\")\n\n" + "2. RESPONSE:\n"
            + " - Use ONLY information from the sources above\n" + " - If you cannot find the information, clearly state it\n"
            + " - Respond in the same language as the user's query\n" + "</rag_context>";

    private static final String PROMPT_NO_CONTEXT = "{{userMessage}}\n\n" + "<rag_context>\n"
            + "There is no relevant information available in the documentation to answer this question.\n"
            + "If the question is a greeting (e.g., Hello, Thank you), simply respond courteously.\n"
            + "If it's a question requiring information, you must respond \"I could not find relevant information in the documentation to answer this question.\"\n"
            + "NEVER respond with your general knowledge for questions requiring documentation.\n" + "Respond in the same language as the user's query.\n"
            + "</rag_context>";

    private static final String VARIABLE_USER_MESSAGE = "userMessage";
    private static final String VARIABLE_CONTENTS = "contents";
    private static final String CONTENT_SEPARATOR = "\n\n";

    private static final PromptTemplate PROMPT_TEMPLATE_WITH_CONTEXT = PromptTemplate.from( PROMPT_WITH_CONTEXT );
    private static final PromptTemplate PROMPT_TEMPLATE_NO_CONTEXT = PromptTemplate.from( PROMPT_NO_CONTEXT );

    /**
     * {@inheritDoc}
     */
    @Override
    public ChatMessage inject( List<Content> contents, ChatMessage chatMessage )
    {
        if ( !( chatMessage instanceof UserMessage ) )
        {
            return chatMessage;
        }

        UserMessage userMessage = (UserMessage) chatMessage;
        String userText = userMessage.singleText( );

        Map<String, Object> variables = new HashMap<>( );
        variables.put( VARIABLE_USER_MESSAGE, userText );

        Prompt prompt;

        if ( contents == null || contents.isEmpty( ) )
        {
            prompt = PROMPT_TEMPLATE_NO_CONTEXT.apply( variables );
        }
        else
        {
            String formattedContents = formatContents( contents );
            variables.put( VARIABLE_CONTENTS, formattedContents );
            prompt = PROMPT_TEMPLATE_WITH_CONTEXT.apply( variables );
        }

        return prompt.toUserMessage( );
    }

    /**
     * Formats content list into a single string separated by double newlines
     * 
     * @param contents
     *            The list of content to format
     * @return The formatted string
     */
    private String formatContents( List<Content> contents )
    {
        return contents.stream( ).map( content -> content.textSegment( ).text( ) ).collect( Collectors.joining( CONTENT_SEPARATOR ) );
    }
}
